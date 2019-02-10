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

import borg.ed.cz.data.ScannedShip;
import borg.ed.cz.data.ScannedShipsListener;
import borg.ed.cz.tasks.DeployFighterTask;
import borg.ed.cz.tasks.EmergencyExitTask;
import borg.ed.cz.tasks.GiveFighterOrderAttackTask;
import borg.ed.cz.tasks.SelectHighestThreatTask;
import borg.ed.cz.tasks.SelectNextHostileTargetTask;
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

	private int nBuggys = 0;
	private int nFighters = 0;
	private int lastFighterDeployed = 0;
	private boolean fighterDeployed = false;
	private boolean fighterRebuilt = true;
	private ScannedShip targetedShip = null;
	private List<ScannedShip> scannedShips = new ArrayList<>();
	private List<ScannedShipsListener> scannedShipsListeners = new ArrayList<>();

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
		if (this.fighterDeployed == false && this.fighterRebuilt == true) {
			logger.info("Deploying a new fighter");
			this.taskPool.execute(this.appctx.getBean(DeployFighterTask.class).setData(nBuggys, nFighters, lastFighterDeployed));

			this.fighterRebuilt = false;
			if (this.lastFighterDeployed == 0) {
				this.lastFighterDeployed = 1;
			} else if (this.lastFighterDeployed == 1 && this.nFighters == 2) {
				this.lastFighterDeployed = 2;
			} else {
				this.lastFighterDeployed = 1;
			}
		}

		// Do we have a good target?
		if (this.lookupMostPromisingShip() == null || (System.currentTimeMillis() - this.lookupMostPromisingShip().getLastSeen()) > 60_000L) {
			// None at all. Cycle through all targets.
			this.taskPool.execute(this.appctx.getBean(SelectNextTargetTask.class));
		} else if (!this.isMostPromisingShipTargeted()) {
			// Yes, but not selected. Cycle through hostile targets.
			this.taskPool.execute(this.appctx.getBean(SelectNextHostileTargetTask.class));
		}
	}

	private boolean isMostPromisingShipTargeted() {
		ScannedShip ship = this.lookupMostPromisingShip();

		return ship != null && ship.equals(this.targetedShip);
	}

	/**
	 * Look for a ship having a bounty. Considers the bounty amount, but also shield and hull health.
	 * 
	 * @return <code>null</code> if none exists at all
	 */
	private ScannedShip lookupMostPromisingShip() {
		if (this.scannedShips.isEmpty()) {
			return null;
		} else {
			List<ScannedShip> bountyShips = this.scannedShips.stream().filter(s -> s.getBounty() != null && s.getBounty().intValue() > 0).collect(Collectors.toList()); // Keep only ship having a bounty
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
			this.fighterDeployed = false;
			this.fighterRebuilt = true;
		} else if (event instanceof FighterDestroyedEvent) {
			this.fighterDeployed = false;
			this.fighterRebuilt = false;
		} else if (event instanceof LaunchFighterEvent) {
			this.fighterDeployed = true;
			this.fighterRebuilt = false;
		} else if (event instanceof FighterRebuiltEvent) {
			this.fighterRebuilt = true;
		} else if (event instanceof CommitCrimeEvent) {
			this.doEmergencyExit("Committed a crime! Emergency exit!");
		} else if (event instanceof LoadoutEvent) {
			this.onLoadoutChange((LoadoutEvent) event);
		}
	}

	private void onLoadoutChange(LoadoutEvent event) {
		this.nBuggys = 0;
		this.nFighters = 0;
		for (Module module : event.getModules()) {
			if (module.getItem().contains("buggybay")) {
				if (module.getItem().contains("size2")) {
					this.nBuggys += 1;
				} else if (module.getItem().contains("size4")) {
					this.nBuggys += 2;
				} else if (module.getItem().contains("size6")) {
					this.nBuggys += 3;
				}
			} else if (module.getItem().contains("fighterbay")) {
				if (module.getItem().contains("size5")) {
					this.nFighters += 1;
				} else if (module.getItem().contains("size6")) {
					this.nFighters += 2;
				} else if (module.getItem().contains("size7")) {
					this.nFighters += 2;
				}
			}
		}
		logger.debug("Loadout changed. Buggys: " + this.nBuggys + "; Fighters: " + this.nFighters);
	}

	private void doEmergencyExit(String message) {
		logger.warn(message);
		this.taskPool.execute(this.appctx.getBean(EmergencyExitTask.class));
		this.shutdown = true;
	}

	private void onShipTargetedEvent(ShipTargetedEvent event) {
		if (!Boolean.TRUE.equals(event.getTargetLocked())) {
			// Target lost
			this.targetedShip = null;
			this.updateScannedShips(this.targetedShip);
		} else {
			// Update data
			this.targetedShip = ScannedShip.fromShipTargetedEvent(event);
			this.updateScannedShips(this.targetedShip);

			// Give a fighter order?
			if (this.isMostPromisingShipTargeted() && this.fighterDeployed) {
				logger.info("Ordering fighter to attack " + this.targetedShip);
				this.taskPool.execute(this.appctx.getBean(GiveFighterOrderAttackTask.class));
			}
		}
	}

	private void onBountyEvent(BountyEvent event) {
		// Remove from scanned ships
		ListIterator<ScannedShip> it = this.scannedShips.listIterator();
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
		if (this.isSameShip(this.targetedShip, shipType, reward)) {
			this.targetedShip = null;
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
		ListIterator<ScannedShip> it = this.scannedShips.listIterator();
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
			this.scannedShips.add(justTargetedShip);
		}

		// Sort
		this.scannedShips = this.sortShips(this.scannedShips);

		// Notify
		for (ScannedShipsListener listener : this.scannedShipsListeners) {
			listener.onNewScannedShips(this.scannedShips);
		}
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

	public boolean addListener(ScannedShipsListener listener) {
		if (listener == null || this.scannedShipsListeners.contains(listener)) {
			return false;
		} else {
			return this.scannedShipsListeners.add(listener);
		}
	}

	public boolean removeListener(ScannedShipsListener listener) {
		if (listener == null) {
			return false;
		} else {
			return this.scannedShipsListeners.remove(listener);
		}
	}

}
