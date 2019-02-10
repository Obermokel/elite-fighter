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

}
