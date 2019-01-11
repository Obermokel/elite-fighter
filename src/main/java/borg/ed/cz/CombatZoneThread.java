/*
 * Author:  Boris Guenther
 * Date:    27.12.2018
 * Time:    15:23:46
 */
package borg.ed.cz;

import java.awt.Robot;
import java.util.concurrent.PriorityBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import borg.ed.cz.activities.Activity;
import borg.ed.universe.journal.JournalUpdateListener;
import borg.ed.universe.journal.Status;
import borg.ed.universe.journal.StatusUpdateListener;
import borg.ed.universe.journal.events.AbstractJournalEvent;
import borg.ed.universe.journal.events.ShipTargetedEvent;

/**
 * CombatZoneThread
 *
 * @author <a href="mailto:boris.guenther@redteclab.com">Boris Guenther</a>
 */
public class CombatZoneThread extends Thread implements JournalUpdateListener, StatusUpdateListener {

    static final Logger logger = LoggerFactory.getLogger(CombatZoneThread.class);

    @Autowired
    private Robot robot;

    private PriorityBlockingQueue<Activity> plannedActivities = new PriorityBlockingQueue<>();

    private GameState gameState = GameState.UNKNOWN;
    private long gameStateSince = 0L;

    public CombatZoneThread() {
        this.setName("CZThread");
        this.setDaemon(false);
    }

    @Override
    public void run() {
        logger.info(this.getName() + " started");

        while (!Thread.currentThread().isInterrupted()) {
            try {
                this.handleGameState();

                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                logger.error(this.getName() + " crashed", e);
                System.exit(-2);
                break; // Quit
            }
        }

        logger.info(this.getName() + " stopped");
    }

    public GameState getGameState() {
        return gameState;
    }

    private void setGameState(GameState gameState) {
        this.gameState = gameState;
        this.gameStateSince = System.currentTimeMillis();
    }

    public long getGameStateSince() {
        return gameStateSince;
    }

    private void handleGameState() {
        switch (this.gameState) {
            case UNKNOWN:
                this.handleGameStateUnknown();
                break;
            case SCAN_FOR_TARGET:
                this.handleGameStateScanForTarget();
                break;
            default:
                break;
        }
    }

    private void handleGameStateUnknown() {
        logger.debug("GameState unknown, scanning for target");
        this.setGameState(GameState.SCAN_FOR_TARGET);
    }

    private void handleGameStateScanForTarget() {
        // TODO Auto-generated method stub

    }

    @Override
    public void onNewJournalEntry(AbstractJournalEvent event) {
        if (event instanceof ShipTargetedEvent) {
            this.onShipTargetedEvent((ShipTargetedEvent) event);
        }
    }

    private void onShipTargetedEvent(ShipTargetedEvent event) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onNewStatus(Status status) {
        // TODO Auto-generated method stub

    }

}
