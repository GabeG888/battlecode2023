package v8;

import battlecode.common.RobotController;

public enum State {

    CHILLING,
    COMPLETE_CONTROL;

    public static State getState (RobotController rc) {
        if(rc.getRobotCount() > rc.getMapWidth() * rc.getMapHeight()/5
                || rc.getRoundNum() > 400 && rc.getRobotCount() > rc.getMapWidth() * rc.getMapHeight()/10) return COMPLETE_CONTROL;
        if(rc.getRoundNum() > 1800) return COMPLETE_CONTROL;
        else return CHILLING;
    }

}
