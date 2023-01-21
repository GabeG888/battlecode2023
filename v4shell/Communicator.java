package v4shell;

import battlecode.common.*;


//COMMUNICATOR STUFF: WHAT ARRAY INDEX DOES WHAT
//IDX 0-3: Keep tracks of our HQ locations
//IDX 4-62: Keeps track of wells:
//16 bits - 12 of them to track well location, 1 for adamantium/mana, last 3 as a counter
//IDX 63: Keeps track of the possible symmetries the map may have
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

    public static int getHQIdx(RobotController rc, MapLocation hq) throws GameActionException {
        for(int i = 0; i < GameConstants.MAX_STARTING_HEADQUARTERS; i++) {
            int encoded = rc.readSharedArray(i) - 1;
            if(hq.x == encoded / 60 && hq.y == encoded % 60) return i;
        }
        System.out.println("No index found.");
        return 0;
    }

    public static void storeWellInfo(RobotController rc, int x, int y, ResourceType rt, int count) throws GameActionException {
        assert rc.canWriteSharedArray(0, 0);
        if(alreadyRecorded(rc, new MapLocation(x, y))) return;
        for(int i = 4; i <= 62; i++) {
            if(rc.readSharedArray(i) == 0) {
                int encoded = x * 60 + y + 1;
                encoded |= (rt == ResourceType.MANA ? 0 : 1) << 12;
                encoded |= count << 13;
                rc.writeSharedArray(i, encoded);
                return;
            }
        }
        System.out.println("All well slots used, could not store well!");
    }

    public static boolean alreadyRecorded(RobotController rc, MapLocation loc) throws GameActionException {
        for(int i = 4; i <= 62; i++) {
            int wellState = rc.readSharedArray(i);
            int encoded = (wellState & 0b0_1111_1111_1111) - 1;
            if(wellState == 0) return false;
            if(encoded / 60 == loc.x && encoded % 60 == loc.y) return true;
        }
        return true;
    }
}
