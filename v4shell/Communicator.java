package v4shell;

import battlecode.common.*;

public class Communicator {

    public static void storeHQLoc(RobotController rc) throws GameActionException {
        assert rc.getRoundNum() == 1;
        for(int i = 0; i < GameConstants.MAX_STARTING_HEADQUARTERS; i++) {
            if(rc.readSharedArray(i) == 0) {
                MapLocation loc = rc.getLocation();
                int encoded = loc.x * 60 + loc.y + 1;
                rc.writeSharedArray(i, encoded);
                return;
            }
        }
    }

    public static MapLocation getHQLoc(RobotController rc, int idx) throws GameActionException {
        int encoded = rc.readSharedArray(idx) - 1;
        int x = encoded / 60, y = encoded % 60;

        return new MapLocation(x, y);
    }


    public static int getHQIdx(RobotController rc, MapLocation hq) throws GameActionException {
        for(int i = 0; i < GameConstants.MAX_STARTING_HEADQUARTERS; i++) {
            int encoded = rc.readSharedArray(i) - 1;
            if(hq.x == encoded / 60 && hq.y == encoded % 60) return i;
        }
        System.out.println("No index found.");
        return 0;
    }

}
