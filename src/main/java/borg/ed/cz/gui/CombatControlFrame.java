/*
 * Author:  Boris Guenther
 * Date:    27.12.2018
 * Time:    15:16:11
 */
package borg.ed.cz.gui;

import java.awt.BorderLayout;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import borg.ed.cz.data.GameStatus;
import borg.ed.cz.data.GameStatusListener;
import borg.ed.cz.data.ScannedShip;
import borg.ed.galaxy.journal.JournalUpdateListener;
import borg.ed.galaxy.journal.Status;
import borg.ed.galaxy.journal.StatusUpdateListener;
import borg.ed.galaxy.journal.events.AbstractJournalEvent;
import borg.ed.galaxy.journal.events.MusicEvent;
import borg.ed.galaxy.journal.events.ReceiveTextEvent;
import borg.ed.galaxy.journal.events.ShipTargetedEvent;

/**
 * CombatControlFrame
 *
 * @author <a href="mailto:boris.guenther@redteclab.com">Boris Guenther</a>
 */
public class CombatControlFrame extends JFrame implements WindowListener, KeyListener, JournalUpdateListener, StatusUpdateListener, GameStatusListener {

	private static final long serialVersionUID = 4349890395389987402L;

	private static final String SPAN_INACTIVE = "<span style=\"color: grey;\">";
	private static final String SPAN_ACTIVE = "<span style=\"color: blue;\">";
	private static final String SPAN_CAUTION = "<span style=\"color: red;\">";
	private static final String SPAN_CLOSE = "</span>";

	private final JLabel lblStatus;
	private final JLabel lblGameStatus;
	private final JLabel lblJournal;
	private final JLabel lblScannedShips;

	private final LinkedList<String> lastJournalMessages = new LinkedList<>();

	public CombatControlFrame() {
		super("EDCZ");

		this.lblStatus = new JLabel();
		this.lblStatus.setVerticalAlignment(SwingConstants.TOP);
		this.lblGameStatus = new JLabel();
		this.lblGameStatus.setVerticalAlignment(SwingConstants.TOP);
		this.lblJournal = new JLabel();
		this.lblJournal.setVerticalAlignment(SwingConstants.TOP);
		this.lblScannedShips = new JLabel();
		this.lblScannedShips.setVerticalAlignment(SwingConstants.TOP);

		this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		this.addWindowListener(this);
		this.setLayout(new BorderLayout());
		this.add(this.lblStatus, BorderLayout.WEST);
		this.add(this.lblGameStatus, BorderLayout.EAST);
		this.add(this.lblJournal, BorderLayout.SOUTH);
		this.add(this.lblScannedShips, BorderLayout.CENTER);
		this.setSize(400, 200);
		this.addKeyListener(this);
	}

	@Override
	public void onNewJournalEntry(AbstractJournalEvent event) {
		if (!this.isIgnoredEvent(event)) {
			synchronized (this.lastJournalMessages) {
				this.lastJournalMessages.addFirst(event.toString());
				while (this.lastJournalMessages.size() > 20) {
					this.lastJournalMessages.removeLast();
				}
				StringBuilder text = new StringBuilder();
				for (String msg : this.lastJournalMessages) {
					text.append(msg + "\n");
				}
				this.lblJournal.setText("<html>" + text.toString().trim().replace("\n", "<br>") + "</html>");
			}
		}
	}

	private boolean isIgnoredEvent(AbstractJournalEvent event) {
		return event instanceof MusicEvent || event instanceof ShipTargetedEvent || event instanceof ReceiveTextEvent;
	}

	@Override
	public void onNewStatus(Status status) {
		if (status == null) {
			this.lblStatus.setText("");
		} else {
			StringBuilder text = new StringBuilder("<html>");

			text.append(status.getTimestamp().format(DateTimeFormatter.ISO_INSTANT)).append("<br>");

			text.append("<br>").append(status.isDocked() ? SPAN_ACTIVE : SPAN_INACTIVE).append("Docked").append(SPAN_CLOSE);
			text.append("<br>").append(status.isLanded() ? SPAN_ACTIVE : SPAN_INACTIVE).append("Landed").append(SPAN_CLOSE);
			text.append("<br>").append(status.isLandingGearDown() ? SPAN_ACTIVE : SPAN_INACTIVE).append("Landing gear down").append(SPAN_CLOSE);
			text.append("<br>").append(status.isShieldsUp() ? SPAN_ACTIVE : SPAN_CAUTION).append("Shields up").append(SPAN_CLOSE);
			text.append("<br>").append(status.isInSupercruise() ? SPAN_ACTIVE : SPAN_INACTIVE).append("In supercruise").append(SPAN_CLOSE);
			text.append("<br>").append(status.isFlightAssistOff() ? SPAN_CAUTION : SPAN_INACTIVE).append("FA off").append(SPAN_CLOSE);
			text.append("<br>").append(status.isHardpointsDeployed() ? SPAN_ACTIVE : SPAN_INACTIVE).append("Hardpoints deployed").append(SPAN_CLOSE);
			text.append("<br>").append(status.isInWing() ? SPAN_ACTIVE : SPAN_INACTIVE).append("In wing").append(SPAN_CLOSE);
			text.append("<br>").append(status.isLightsOn() ? SPAN_ACTIVE : SPAN_INACTIVE).append("Lights on").append(SPAN_CLOSE);
			text.append("<br>").append(status.isCargoScoopDeployed() ? SPAN_ACTIVE : SPAN_INACTIVE).append("Cargo scoop deployed").append(SPAN_CLOSE);
			text.append("<br>").append(status.isSilentRunning() ? SPAN_CAUTION : SPAN_INACTIVE).append("Silent running").append(SPAN_CLOSE);
			text.append("<br>").append(status.isScoopingFuel() ? SPAN_ACTIVE : SPAN_INACTIVE).append("Scooping fuel").append(SPAN_CLOSE);
			text.append("<br>").append(status.isSrvHandbrake() ? SPAN_ACTIVE : SPAN_INACTIVE).append("SRV handbrake").append(SPAN_CLOSE);
			text.append("<br>").append(status.isSrvTurret() ? SPAN_ACTIVE : SPAN_INACTIVE).append("SRV turret").append(SPAN_CLOSE);
			text.append("<br>").append(status.isSrvUnderShip() ? SPAN_ACTIVE : SPAN_INACTIVE).append("SRV under ship").append(SPAN_CLOSE);
			text.append("<br>").append(status.isSrvDriveAssistOn() ? SPAN_ACTIVE : SPAN_INACTIVE).append("SRV drive assist").append(SPAN_CLOSE);
			text.append("<br>").append(status.isFsdMassLocked() ? SPAN_ACTIVE : SPAN_INACTIVE).append("FSD mass locked").append(SPAN_CLOSE);
			text.append("<br>").append(status.isFsdCharging() ? SPAN_ACTIVE : SPAN_INACTIVE).append("FSD charging").append(SPAN_CLOSE);
			text.append("<br>").append(status.isFsdCooldown() ? SPAN_ACTIVE : SPAN_INACTIVE).append("FSD cooldown").append(SPAN_CLOSE);
			text.append("<br>").append(status.isLowFuel() ? SPAN_CAUTION : SPAN_INACTIVE).append("Low fuel").append(SPAN_CLOSE);
			text.append("<br>").append(status.isOverHeating() ? SPAN_CAUTION : SPAN_INACTIVE).append("Overheating").append(SPAN_CLOSE);
			text.append("<br>").append(status.hasLatLon() ? SPAN_ACTIVE : SPAN_INACTIVE).append("Has lat/lon").append(SPAN_CLOSE);
			text.append("<br>").append(status.isInDanger() ? SPAN_CAUTION : SPAN_INACTIVE).append("In danger").append(SPAN_CLOSE);
			text.append("<br>").append(status.isBeingInterdicted() ? SPAN_CAUTION : SPAN_INACTIVE).append("Being interdicted").append(SPAN_CLOSE);
			text.append("<br>").append(status.isInMothership() ? SPAN_ACTIVE : SPAN_INACTIVE).append("In mothership").append(SPAN_CLOSE);
			text.append("<br>").append(status.isInFighter() ? SPAN_ACTIVE : SPAN_INACTIVE).append("In fighter").append(SPAN_CLOSE);
			text.append("<br>").append(status.isInSrv() ? SPAN_ACTIVE : SPAN_INACTIVE).append("In SRV").append(SPAN_CLOSE);

			if ((status.getLatitude() != null && status.getLongitude() != null) || status.getHeading() != null || status.getAltitude() != null) {
				text.append("<br>");
			}
			if (status.getLatitude() != null && status.getLongitude() != null) {
				text.append("<br>Lat/Lon: " + status.getLatitude() + " / " + status.getLongitude());
			}
			if (status.getHeading() != null) {
				text.append("<br>Heading: " + status.getHeading() + "°");
			}
			if (status.getAltitude() != null) {
				text.append("<br>Altitude: " + status.getAltitude().divide(new BigDecimal(1000), 2, BigDecimal.ROUND_HALF_UP) + " km");
			}

			text.append("</html>");

			this.lblStatus.setText(text.toString());
		}
	}

	@Override
	public void onNewGameStatus(GameStatus status) {
		StringBuilder scannedShips = new StringBuilder();
		for (ScannedShip ship : status.getScannedShips()) {
			scannedShips.append(ship.toHtmlString() + "\n");
		}
		this.lblScannedShips.setText("<html>" + scannedShips.toString().trim().replace("\n", "<br>") + "</html>");

		StringBuilder text = new StringBuilder("<html>");

		text.append(ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT)).append("<br>");

		text.append("<br>").append("Buggys: ").append(status.getNBuggys());
		text.append("<br>").append("Fighters: ").append(status.getNFighters());
		text.append("<br>").append("Last fighter: ").append(status.getLastFighterDeployed());
		text.append("<br>").append(status.isFighterDeployed() ? SPAN_ACTIVE : SPAN_INACTIVE).append("Fighter deployed").append(SPAN_CLOSE);
		text.append("<br>").append(status.isFighterRebuilt() ? SPAN_ACTIVE : SPAN_INACTIVE).append("Fighter rebuilt").append(SPAN_CLOSE);

		text.append("</html>");

		this.lblGameStatus.setText(text.toString());
	}

	@Override
	public void windowOpened(WindowEvent e) {
		// Do nothing
	}

	@Override
	public void windowClosing(WindowEvent e) {
		// Do nothing
	}

	@Override
	public void windowClosed(WindowEvent e) {
		System.exit(0);
	}

	@Override
	public void windowIconified(WindowEvent e) {
		// Do nothing
	}

	@Override
	public void windowDeiconified(WindowEvent e) {
		// Do nothing
	}

	@Override
	public void windowActivated(WindowEvent e) {
		// Do nothing
	}

	@Override
	public void windowDeactivated(WindowEvent e) {
		// Do nothing
	}

	@Override
	public void keyTyped(KeyEvent e) {
		// Do nothing
	}

	@Override
	public void keyPressed(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
			System.exit(-1);
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
		// Do nothing
	}

}
