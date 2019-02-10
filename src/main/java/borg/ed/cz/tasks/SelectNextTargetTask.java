package borg.ed.cz.tasks;

import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import borg.ed.galaxy.robot.ShipControl;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class SelectNextTargetTask extends Thread {

	@Autowired
	private ShipControl shipControl = null;

	private final Random random = new Random();

	private static long lastExecuted = 0L;

	@Override
	public void run() {
		try {
			long now = System.currentTimeMillis();
			if (now - lastExecuted > 10000) {
				lastExecuted = now;
				Thread.sleep(this.shipControl.selectNextTarget() + 50 + this.random.nextInt(100));
			}
		} catch (InterruptedException e) {
			// Quit
		}
	}

}
