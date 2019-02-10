package borg.ed.cz.tasks;

import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import borg.ed.galaxy.robot.ShipControl;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class DeployFighterTask extends Thread {

	static final Logger logger = LoggerFactory.getLogger(DeployFighterTask.class);

	@Autowired
	private ShipControl shipControl = null;

	private final Random random = new Random();

	private int nBuggys = 0;
	private int nFighters = 0;
	private int lastFighterDeployed = 0;

	public DeployFighterTask setData(int nBuggys, int nFighters, int lastFighterDeployed) {
		this.nBuggys = nBuggys;
		this.nFighters = nFighters;
		this.lastFighterDeployed = lastFighterDeployed;

		return this;
	}

	@Override
	public void run() {
		int nBack = 0;

		try {
			if (this.nFighters <= 0) {
				logger.warn("No fighters available");
			} else {
				// Open role panel
				nBack++;
				Thread.sleep(this.shipControl.uiBottomPanel() + 50 + this.random.nextInt(100));

				//				// Skip over buggys
				//				if (this.lastFighterDeployed == 0) {
				//					for (int n = 0; n < this.nBuggys; n++) {
				//						Thread.sleep(this.shipControl.uiDown() + 50 + this.random.nextInt(100));
				//					}
				//				}

				// Hover over fighter to deploy
				if (this.lastFighterDeployed == 0) {
					Thread.sleep(this.shipControl.uiDown() + 50 + this.random.nextInt(100));
				} else if (this.lastFighterDeployed == 1 && this.nFighters == 2) {
					Thread.sleep(this.shipControl.uiDown() + 50 + this.random.nextInt(100));
				} else if (this.lastFighterDeployed == 2 && this.nFighters == 2) {
					Thread.sleep(this.shipControl.uiUp() + 50 + this.random.nextInt(100));
				}

				// Select fighter, click on deploy
				nBack++;
				Thread.sleep(this.shipControl.uiSelect() + 50 + this.random.nextInt(100));
				Thread.sleep(this.shipControl.uiSelect() + 50 + this.random.nextInt(100));

				// If this is the first time move down to select the NPC
				if (this.lastFighterDeployed == 0) {
					Thread.sleep(this.shipControl.uiDown() + 50 + this.random.nextInt(100));
				}

				// Select to actually deploy the fighter
				Thread.sleep(this.shipControl.uiSelect() + 50 + this.random.nextInt(100));

				// Close the role panel
				Thread.sleep(this.shipControl.uiBack() + 50 + this.random.nextInt(100));
			}
		} catch (InterruptedException e) {
			// UI back for n times, then quit
			for (int n = 0; n < nBack; n++) {
				try {
					Thread.sleep(this.shipControl.uiBack() + 50 + this.random.nextInt(100));
				} catch (InterruptedException e1) {
					// Ignore
				}
			}
		}
	}

}
