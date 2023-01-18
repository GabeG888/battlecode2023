package v4shell;

import battlecode.common.*;

import java.util.*;

public class Headquarters {
    public static void run(RobotController rc) throws GameActionException {
        if(rc.getRoundNum() == 1) Communicator.storeHQLoc(rc);

    }

}
