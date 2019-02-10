/*
 * Author:  Boris Guenther
 * Date:    27.12.2018
 * Time:    16:56:25
 */
package borg.ed.cz.data;

import java.math.BigDecimal;
import java.util.Locale;

import borg.ed.galaxy.journal.events.ShipTargetedEvent;
import borg.ed.galaxy.util.MiscUtil;
import lombok.Getter;
import lombok.Setter;

/**
 * TargetedShip
 *
 * @author <a href="mailto:boris.guenther@redteclab.com">Boris Guenther</a>
 */
@Getter
@Setter
public class ScannedShip {

	/**
	 * Available from: Stage 0
	 */
	private String ship = null;

	/**
	 * Available from: Stage 1
	 */
	private String pilotName = null;

	/**
	 * Available from: Stage 1
	 */
	private String pilotRank = null;

	/**
	 * Available from: Stage 2 (0.00 to 100.00)
	 */
	private BigDecimal ShieldHealth = null;

	/**
	 * Available from: Stage 2 (0.00 to 100.00)
	 */
	private BigDecimal HullHealth = null;

	/**
	 * Available from: Stage 3
	 */
	private BigDecimal bounty = null;

	private long lastSeen = System.currentTimeMillis();

	public static ScannedShip fromShipTargetedEvent(ShipTargetedEvent event) {
		ScannedShip ship = new ScannedShip();
		ship.setShip(event.getShip());
		ship.setPilotName(event.getPilotName_Localised());
		ship.setPilotRank(event.getPilotRank());
		ship.setShieldHealth(event.getShieldHealth());
		ship.setHullHealth(event.getHullHealth());
		ship.setBounty(event.getBounty());
		ship.setLastSeen(event.getTimestamp().toInstant().toEpochMilli());
		return ship;
	}

	@Override
	public String toString() {
		return String.format(Locale.US, "%s %s (%s, %,d CR bounty, S:%d%%, H:%d%%)", // Deadly Anaconda (Harry Potter, 241,500 CR bounty, S:62%, H:100%)
				MiscUtil.getAsString(this.pilotRank, "??RANK??"), // rank
				MiscUtil.getAsString(this.ship, "??SHIP??"), // ship
				MiscUtil.getAsString(this.pilotName, "??PILOT??"), // pilot
				MiscUtil.getAsInt(this.bounty, 0), // bounty
				MiscUtil.getAsInt(this.ShieldHealth, null), // shield
				MiscUtil.getAsInt(this.HullHealth, null)); // hull
	}

	public String toHtmlString() {
		String bountyRed = "<span style=\"color: " + red(MiscUtil.getAsInt(this.bounty, 0) / 200_000f) + ";\">";
		String shieldBlue = "<span style=\"color: " + blue(MiscUtil.getAsInt(this.ShieldHealth, 100) / 100f) + ";\">";
		String hullOrange = "<span style=\"color: " + orange(MiscUtil.getAsInt(this.HullHealth, 100) / 100f) + ";\">";
		return String.format(Locale.US, "%s %s (%s, " + bountyRed + "%,d CR bounty</span>, " + shieldBlue + "S:%d%%</span>, " + hullOrange + "H:%d%%</span>)", // Deadly Anaconda (Harry Potter, 241,500 CR bounty, S:62%, H:100%)
				MiscUtil.getAsString(this.pilotRank, "??RANK??"), // rank
				MiscUtil.getAsString(this.ship, "??SHIP??"), // ship
				MiscUtil.getAsString(this.pilotName, "??PILOT??"), // pilot
				MiscUtil.getAsInt(this.bounty, 0), // bounty
				MiscUtil.getAsInt(this.ShieldHealth, null), // shield
				MiscUtil.getAsInt(this.HullHealth, null)); // hull
	}

	private static String red(float percent) {
		int value = Math.max(0, Math.min(255, (int) (percent * 255f)));
		String hex = Integer.toHexString(value);
		return "#" + hex + "8888";
	}

	private static String blue(float percent) {
		int value = Math.max(0, Math.min(255, (int) (percent * 255f)));
		String hex = Integer.toHexString(value);
		return "#8888" + hex;
	}

	private static String orange(float percent) {
		int redValue = Math.max(0, Math.min(255, (int) (percent * 255f)));
		String redHex = Integer.toHexString(redValue);
		String greenHex = Integer.toHexString(redValue / 2);
		return "#" + redHex + greenHex + "88";
	}

	public String toEqualsString() {
		return "TargetedShip [ship=" + ship + ", pilotName=" + pilotName + ", pilotRank=" + pilotRank + "]";
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ScannedShip other = (ScannedShip) obj;
		return this.toEqualsString().equals(other.toEqualsString());
	}

	@Override
	public int hashCode() {
		return this.toEqualsString().hashCode();
	}

}
