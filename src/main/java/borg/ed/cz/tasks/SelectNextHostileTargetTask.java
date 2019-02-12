package borg.ed.cz.tasks;

import java.util.LinkedList;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import borg.ed.galaxy.robot.ShipControl;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class SelectNextHostileTargetTask extends Thread {

    @Autowired
    private ShipControl shipControl = null;

    private final Random random = new Random();

    private static final LinkedList<Long> lastExecutionTimes = new LinkedList<>();

    @Override
    public void run() {
        try {
            if (isNextExecutionAllowed()) {
                synchronized (lastExecutionTimes) {
                    lastExecutionTimes.addLast(System.currentTimeMillis());
                }
                Thread.sleep(this.shipControl.selectNextHostileTarget() + 50 + this.random.nextInt(100));
            }
        } catch (InterruptedException e) {
            // Quit
        }
    }

    public static boolean isNextExecutionAllowed() {
        // Max 20 per minute AND max 1 per 500 ms
        final long now = System.currentTimeMillis();
        final long nowMinusOneMinute = now - 60_000L;
        synchronized (lastExecutionTimes) {
            lastExecutionTimes.removeIf(ts -> ts < nowMinusOneMinute);
            if (lastExecutionTimes.isEmpty()) {
                return true;
            } else if (now - lastExecutionTimes.getLast() < 500) {
                return false;
            } else {
                return lastExecutionTimes.size() < 20;
            }
        }
    }

}
