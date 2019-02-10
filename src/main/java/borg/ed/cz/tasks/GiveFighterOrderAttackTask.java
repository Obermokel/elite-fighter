package borg.ed.cz.tasks;

import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import borg.ed.galaxy.robot.ShipControl;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class GiveFighterOrderAttackTask extends Thread {

	@Autowired
	private ShipControl shipControl = null;

	private final Random random = new Random();

	@Override
	public void run() {
		try {
			Thread.sleep(this.shipControl.fighterOrderAttack() + 50 + this.random.nextInt(100));
		} catch (InterruptedException e) {
			// Quit
		}
	}

}
