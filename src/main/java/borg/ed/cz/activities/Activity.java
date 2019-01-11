/*
 * Author:  Boris Guenther
 * Date:    28.12.2018
 * Time:    13:23:05
 */
package borg.ed.cz.activities;

import borg.ed.cz.data.TargetedShip;

/**
 * Activity
 *
 * @author <a href="mailto:boris.guenther@redteclab.com">Boris Guenther</a>
 */
public class Activity {

    private final Action action;

    private TargetedShip ship = null;

    public Activity(Action action) {
        this.action = action;
    }

    public Integer getPriority() {
        switch (this.action) {
            case EMERGENCY_EXIT:
                return Integer.MIN_VALUE;
            case DEPLOY_FIGHTER:
                return 0;
            case DESTROY_SHIP:
                return 1;
            default:
                break;
        }
        return Integer.MAX_VALUE;
    }

}
