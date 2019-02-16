/*
 * Author:  Boris Guenther
 * Date:    27.12.2018
 * Time:    15:23:46
 */
package borg.ed.cz;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import borg.ed.cz.data.GameStatus;
import borg.ed.cz.data.GameStatusListener;
import borg.ed.cz.data.ScannedShip;
import borg.ed.cz.tasks.DeployFighterTask;
import borg.ed.cz.tasks.EmergencyExitTask;
import borg.ed.cz.tasks.GiveFighterOrderAttackTask;
import borg.ed.cz.tasks.SelectHighestThreatTask;
import borg.ed.cz.tasks.SelectNextTargetTask;
import borg.ed.galaxy.journal.JournalUpdateListener;
import borg.ed.galaxy.journal.Status;
import borg.ed.galaxy.journal.StatusUpdateListener;
import borg.ed.galaxy.journal.events.AbstractJournalEvent;
import borg.ed.galaxy.journal.events.BountyEvent;
import borg.ed.galaxy.journal.events.CommitCrimeEvent;
import borg.ed.galaxy.journal.events.DockFighterEvent;
import borg.ed.galaxy.journal.events.FighterDestroyedEvent;
import borg.ed.galaxy.journal.events.FighterRebuiltEvent;
import borg.ed.galaxy.journal.events.LaunchFighterEvent;
import borg.ed.galaxy.journal.events.LoadoutEvent;
import borg.ed.galaxy.journal.events.LoadoutEvent.Module;
import borg.ed.galaxy.journal.events.ShipTargetedEvent;
import borg.ed.galaxy.journal.events.UnderAttackEvent;
import borg.ed.galaxy.util.MiscUtil;

/**
 * CombatZoneThread
 *
 * @author <a href="mailto:boris.guenther@redteclab.com">Boris Guenther</a>
 */
public class CombatZoneThread extends Thread implements JournalUpdateListener, StatusUpdateListener {

	static final Logger logger = LoggerFactory.getLogger(CombatZoneThread.class);

	public volatile boolean shutdown = false;

	private final ExecutorService taskPool = Executors.newFixedThreadPool(1);

	@Autowired
	private ApplicationContext appctx = null;

	private GameStatus gameStatus = new GameStatus();

	private List<GameStatusListener> gameStatusListeners = new ArrayList<>();

	public CombatZoneThread() {
		this.setName("CZThread");
		this.setDaemon(false);
	}

	@Override
	public void run() {
		logger.info(this.getName() + " started");

		while (!Thread.currentThread().isInterrupted() && !this.shutdown) {
			try {
				this.handleGameState();

				Thread.sleep(50);
			} catch (InterruptedException e) {
				logger.info(this.getName() + " has been interrupted");
				this.shutdown = true;
			} catch (Exception e) {
				logger.error(this.getName() + " crashed", e);
				this.doEmergencyExit("Application crashed: " + e);
			}
		}

		this.taskPool.shutdown();
		try {
			if (!this.taskPool.awaitTermination(1, TimeUnit.MINUTES)) {
				this.taskPool.shutdownNow();
			}
		} catch (InterruptedException e) {
			this.taskPool.shutdownNow();
		}

		logger.info(this.getName() + " stopped");
	}

	private void handleGameState() {
		// Can we deploy a fighter?
		if (this.gameStatus.isFighterDeployed() == false && this.gameStatus.isFighterRebuilt() == true) {
			logger.info("Deploying a new fighter");
			this.taskPool.execute(this.appctx.getBean(DeployFighterTask.class).setData(this.gameStatus.getNBuggys(), this.gameStatus.getNFighters(), this.gameStatus.getLastFighterDeployed()));

			this.gameStatus.setFighterRebuilt(false);
			if (this.gameStatus.getLastFighterDeployed() == 0) {
				this.gameStatus.setLastFighterDeployed(1);
			} else if (this.gameStatus.getLastFighterDeployed() == 1 && this.gameStatus.getNFighters() == 2) {
				this.gameStatus.setLastFighterDeployed(2);
			} else {
				this.gameStatus.setLastFighterDeployed(1);
			}
		}

		// Do we have a good target?
		if (SelectNextTargetTask.isNextExecutionAllowed()) {
			long currentTargetMillis = this.gameStatus.currentTargetMillis();
			int currentTargetScanStage = this.gameStatus.currentTargetScanStage();
			if (currentTargetScanStage == 3 && currentTargetMillis > 1_000L) {
				//logger.debug("SelectNextTarget: Current target " + this.gameStatus.getTargetedShip() + " is already at scan stage 3");
				this.taskPool.execute(this.appctx.getBean(SelectNextTargetTask.class));
			} else if (currentTargetMillis == Long.MAX_VALUE) {
				//logger.debug("SelectNextTarget: We do not have a current target");
				this.taskPool.execute(this.appctx.getBean(SelectNextTargetTask.class));
			} else if (currentTargetMillis > 10_000L) {
				//logger.debug("SelectNextTarget: Current target " + this.gameStatus.getTargetedShip() + " has been focused for 10 seconds");
				this.taskPool.execute(this.appctx.getBean(SelectNextTargetTask.class));
			}
		}
	}

	private boolean isMostPromisingShipTargeted() {
		ScannedShip ship = this.lookupMostPromisingShip();

		return ship != null && ship.equals(this.gameStatus.getTargetedShip());
	}

	/**
	 * Look for a ship having a bounty. Considers the bounty amount, but also shield and hull health.
	 * 
	 * @return <code>null</code> if none exists at all
	 */
	private ScannedShip lookupMostPromisingShip() {
		if (this.gameStatus.getScannedShips().isEmpty()) {
			return null;
		} else {
			List<ScannedShip> bountyShips = this.gameStatus.getScannedShips().stream().filter(s -> s.getBounty() != null && s.getBounty().intValue() > 0).collect(Collectors.toList()); // Keep only ship having a bounty
			List<ScannedShip> sortedShips = this.sortShips(bountyShips);

			if (sortedShips.isEmpty()) {
				return null;
			} else {
				return sortedShips.get(0);
			}
		}
	}

	@Override
	public void onNewJournalEntry(AbstractJournalEvent event) {
		if (event instanceof ShipTargetedEvent) {
			this.onShipTargetedEvent((ShipTargetedEvent) event);
		} else if (event instanceof BountyEvent) {
			this.onBountyEvent((BountyEvent) event);
		} else if (event instanceof UnderAttackEvent) {
			logger.info("Under attack! Selecting highest threat.");
			this.taskPool.execute(this.appctx.getBean(SelectHighestThreatTask.class));
		} else if (event instanceof DockFighterEvent) {
			this.gameStatus.setFighterDeployed(false);
			this.gameStatus.setFighterRebuilt(true);
			this.invokeGameStatusListeners();
		} else if (event instanceof FighterDestroyedEvent) {
			this.gameStatus.setFighterDeployed(false);
			this.gameStatus.setFighterRebuilt(false);
			this.invokeGameStatusListeners();
		} else if (event instanceof LaunchFighterEvent) {
			this.gameStatus.setFighterDeployed(true);
			this.gameStatus.setFighterRebuilt(false);
			this.invokeGameStatusListeners();
		} else if (event instanceof FighterRebuiltEvent) {
			this.gameStatus.setFighterRebuilt(true);
			this.invokeGameStatusListeners();
		} else if (event instanceof CommitCrimeEvent) {
			this.doEmergencyExit("Committed a crime! Emergency exit!");
		} else if (event instanceof LoadoutEvent) {
			this.onLoadoutChange((LoadoutEvent) event);
		}
	}

	private void onLoadoutChange(LoadoutEvent event) {
		int nBuggys = 0;
		int nFighters = 0;
		for (Module module : event.getModules()) {
			String item = module.getItem().toLowerCase();
			if (item.contains("buggybay")) {
				if (item.contains("size2")) {
					nBuggys += 1;
				} else if (item.contains("size4")) {
					nBuggys += 2;
				} else if (item.contains("size6")) {
					nBuggys += 3;
				}
			} else if (item.contains("fighterbay")) {
				if (item.contains("size5")) {
					nFighters += 1;
				} else if (item.contains("size6")) {
					nFighters += 2;
				} else if (item.contains("size7")) {
					nFighters += 2;
				}
			}
		}
		this.gameStatus.setNBuggys(nBuggys);
		this.gameStatus.setNFighters(nFighters);
		logger.debug("Loadout changed. Buggys: " + nBuggys + "; Fighters: " + nFighters);
		this.invokeGameStatusListeners();
	}

	private void doEmergencyExit(String message) {
		logger.warn(message);
		this.taskPool.execute(this.appctx.getBean(EmergencyExitTask.class));
		this.shutdown = true;
	}

	private void onShipTargetedEvent(ShipTargetedEvent event) {
		if (!Boolean.TRUE.equals(event.getTargetLocked())) {
			// Target lost
			this.gameStatus.setTargetedShip(null);
			this.updateScannedShips(this.gameStatus.getTargetedShip());
		} else {
			// Update data
			this.gameStatus.setTargetedShip(ScannedShip.fromShipTargetedEvent(event));
			this.updateScannedShips(this.gameStatus.getTargetedShip());

			// Give a fighter order?
			if (this.gameStatus.isFighterDeployed()) {
				if (this.isMostPromisingShipTargeted()) {
					logger.info("Ordering fighter to attack most promising target " + this.gameStatus.getTargetedShip());
					this.gameStatus.setAttackedShip(this.gameStatus.getTargetedShip());
					this.taskPool.execute(this.appctx.getBean(GiveFighterOrderAttackTask.class));
				} else if (MiscUtil.getAsInt(this.gameStatus.getTargetedShip().getBounty(), 0) > 0 && this.gameStatus.getAttackedShip() == null) {
					logger.info("Ordering fighter to attack " + this.gameStatus.getTargetedShip());
					this.gameStatus.setAttackedShip(this.gameStatus.getTargetedShip());
					this.taskPool.execute(this.appctx.getBean(GiveFighterOrderAttackTask.class));
				}
			}
		}
	}

	private void onBountyEvent(BountyEvent event) {
		// Remove from scanned ships
		ListIterator<ScannedShip> it = this.gameStatus.getScannedShips().listIterator();
		String shipType = MiscUtil.getAsString(event.getTarget(), "unknown");
		int reward = MiscUtil.getAsInt(event.getTotalReward(), 0);
		while (it.hasNext()) {
			ScannedShip ship = it.next();
			if (this.isSameShip(ship, shipType, reward)) {
				logger.debug("It seems like we have killed " + ship);
				it.remove();
			}
		}

		// Also reset target
		if (this.isSameShip(this.gameStatus.getTargetedShip(), shipType, reward)) {
			this.gameStatus.setTargetedShip(null);
		}
		if (this.isSameShip(this.gameStatus.getAttackedShip(), shipType, reward)) {
			this.gameStatus.setAttackedShip(null);
		}
	}

	private boolean isSameShip(ScannedShip scannedShip, String shipType, int bounty) {
		if (scannedShip != null && shipType != null) {
			if (shipType.equals(scannedShip.getShip())) {
				float bountyDelta = Math.abs(bounty - MiscUtil.getAsInt(scannedShip.getBounty(), 0));
				float bountyOff = bountyDelta / bounty;
				return bountyOff <= 0.1f;
			}
		}
		return false;
	}

	private void updateScannedShips(ScannedShip justTargetedShip) {
		// Remove outdated ships
		final long now = System.currentTimeMillis();
		ListIterator<ScannedShip> it = this.gameStatus.getScannedShips().listIterator();
		while (it.hasNext()) {
			ScannedShip ship = it.next();
			if (now - ship.getLastSeen() > 300_000) {
				it.remove(); // Not seen for 5 minutes
			} else if (ship.equals(justTargetedShip)) {
				it.remove(); // Will be re-added with updated information
			}
		}

		// Add the new ship
		if (justTargetedShip != null) {
			this.gameStatus.getScannedShips().add(justTargetedShip);
		}

		// Sort
		this.gameStatus.setScannedShips(this.sortShips(this.gameStatus.getScannedShips()));

		// Notify
		this.invokeGameStatusListeners();
	}

	private List<ScannedShip> sortShips(List<ScannedShip> ships) {
		List<ScannedShip> result = new ArrayList<>(ships);

		// Put ships with low shields in front
		Collections.sort(result, new Comparator<ScannedShip>() {
			@Override
			public int compare(ScannedShip ship1, ScannedShip ship2) {
				BigDecimal shield1 = MiscUtil.getAsBigDecimal(ship1.getShieldHealth(), new BigDecimal(100)).divide(new BigDecimal(10), 0, BigDecimal.ROUND_HALF_UP);
				BigDecimal shield2 = MiscUtil.getAsBigDecimal(ship2.getShieldHealth(), new BigDecimal(100)).divide(new BigDecimal(10), 0, BigDecimal.ROUND_HALF_UP);
				return shield1.compareTo(shield2);
			}
		});

		// Put ships with low hull in front
		Collections.sort(result, new Comparator<ScannedShip>() {
			@Override
			public int compare(ScannedShip ship1, ScannedShip ship2) {
				BigDecimal hull1 = MiscUtil.getAsBigDecimal(ship1.getHullHealth(), new BigDecimal(100)).divide(new BigDecimal(10), 0, BigDecimal.ROUND_HALF_UP);
				BigDecimal hull2 = MiscUtil.getAsBigDecimal(ship2.getHullHealth(), new BigDecimal(100)).divide(new BigDecimal(10), 0, BigDecimal.ROUND_HALF_UP);
				return hull1.compareTo(hull2);
			}
		});

		// Put ships with a high bounty (in 50k steps) in front
		Collections.sort(result, new Comparator<ScannedShip>() {
			@Override
			public int compare(ScannedShip ship1, ScannedShip ship2) {
				BigDecimal bounty1 = MiscUtil.getAsBigDecimal(ship1.getBounty(), BigDecimal.ZERO).divide(new BigDecimal(50_000), 0, BigDecimal.ROUND_HALF_UP);
				BigDecimal bounty2 = MiscUtil.getAsBigDecimal(ship2.getBounty(), BigDecimal.ZERO).divide(new BigDecimal(50_000), 0, BigDecimal.ROUND_HALF_UP);
				return bounty2.compareTo(bounty1);
			}
		});

		return result;
	}

	@Override
	public void onNewStatus(Status status) {
		if (!status.isShieldsUp()) {
			this.doEmergencyExit("Lost shields! Emergency exit!");
		} else if (status.isLowFuel()) {
			this.doEmergencyExit("Low on fuel! Emergency exit!");
		}
	}

	public boolean addListener(GameStatusListener listener) {
		if (listener == null || this.gameStatusListeners.contains(listener)) {
			return false;
		} else {
			return this.gameStatusListeners.add(listener);
		}
	}

	public boolean removeListener(GameStatusListener listener) {
		if (listener == null) {
			return false;
		} else {
			return this.gameStatusListeners.remove(listener);
		}
	}

	private void invokeGameStatusListeners() {
		for (GameStatusListener listener : this.gameStatusListeners) {
			listener.onNewGameStatus(this.gameStatus);
		}
	}

}
