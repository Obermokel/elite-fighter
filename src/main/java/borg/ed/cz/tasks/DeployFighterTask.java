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
				logger.debug("Open role panel");
				Thread.sleep(this.shipControl.uiBottomPanel() + 2000 + this.random.nextInt(100));

				//				// Skip over buggys
				//				if (this.lastFighterDeployed == 0) {
				//					for (int n = 0; n < this.nBuggys; n++) {
				//						Thread.sleep(this.shipControl.uiDown() + 50 + this.random.nextInt(100));
				//					}
				//				}

				// Hover over fighter to deploy
				if (this.lastFighterDeployed == 0) {
					logger.debug("First time deploy: 1x down");
					Thread.sleep(this.shipControl.uiDown() + 2000 + this.random.nextInt(100));
				} else if (this.lastFighterDeployed == 1 && this.nFighters == 2) {
					logger.debug("Alternate deploy: 1x down");
					Thread.sleep(this.shipControl.uiDown() + 2000 + this.random.nextInt(100));
				} else if (this.lastFighterDeployed == 2 && this.nFighters == 2) {
					logger.debug("Alternate deploy: 1x up");
					Thread.sleep(this.shipControl.uiUp() + 2000 + this.random.nextInt(100));
				}

				// Select fighter, click on deploy
				nBack++;
				logger.debug("Click on fighter");
				Thread.sleep(this.shipControl.uiSelect() + 2000 + this.random.nextInt(100));
				logger.debug("Click on deploy");
				Thread.sleep(this.shipControl.uiSelect() + 2000 + this.random.nextInt(100));

				// If this is the first time move down to select the NPC
				if (this.lastFighterDeployed == 0) {
					logger.debug("First time deploy: 1x down for NPC");
					Thread.sleep(this.shipControl.uiDown() + 2000 + this.random.nextInt(100));
				}

				// Select to actually deploy the fighter
				logger.debug("Click on pilot");
				Thread.sleep(this.shipControl.uiSelect() + 2000 + this.random.nextInt(100));

				// Close the role panel
				logger.debug("Close role panel");
				Thread.sleep(this.shipControl.uiBottomPanel() + 2000 + this.random.nextInt(100));
				logger.debug("UI back");
				Thread.sleep(this.shipControl.uiBack() + 2000 + this.random.nextInt(100));
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
