package v4shell;

import battlecode.common.*;

import java.util.*;

public class Headquarters {

    static void detectWells(RobotController rc) throws GameActionException {
        for(WellInfo wi : rc.senseNearbyWells()) {
            MapLocation loc = wi.getMapLocation();
            ResourceType rt = wi.getResourceType();

            Communicator.storeWellInfo(rc, loc.x, loc.y, rt);
        }
    }

    public static void run(RobotController rc) throws GameActionException {
        if(rc.getRoundNum() == 1) {
            Communicator.storeHQLoc(rc);
            detectWells(rc);
        }
        if(rc.getRoundNum() == 2 && rc.readSharedArray(63) == 0) MapStore.computeInitialSymmetry(rc);

    }

}
