package borg.ed.cz.data;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GameStatus {

	private int nBuggys = 0;
	private int nFighters = 0;
	private int lastFighterDeployed = 0;
	private boolean fighterDeployed = false;
	private boolean fighterRebuilt = true;
	private ScannedShip targetedShip = null;
	private List<ScannedShip> scannedShips = new ArrayList<>();
	
	/**
	 * Check for how long the current target is selected.
	 * 
	 * @return <code>Long.MAX_VALUE</code> if we do not have a target.
	 */
	public long currentTargetMillis() {
	    if(this.targetedShip == null) {
	        return Long.MAX_VALUE;
	    } else {
	        return System.currentTimeMillis() - this.targetedShip.getLastSeen();
	    }
	}
	
	/**
	 * Get the scan stage of the current target.
	 * 
	 * @return 0 if we do not have a target.
	 */
	public int currentTargetScanStage() {
	    if(this.targetedShip == null) {
	        return 0;
	    } else {
	        return this.targetedShip.getScanStage();
	    }
	}

}
