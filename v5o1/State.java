package v5o1;

import battlecode.common.RobotController;

public enum State {

    CHILLING,
    COMPLETE_CONTROL;

    public static State getState (RobotController rc) {
        if(rc.getRobotCount() > rc.getMapWidth() * rc.getMapHeight()/6
                || rc.getRoundNum() > 600 && rc.getRobotCount() > 150) return COMPLETE_CONTROL;
        if(rc.getRoundNum() > 1800) return COMPLETE_CONTROL;
        else return CHILLING;
    }

}
