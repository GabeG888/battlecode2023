package v7;

import battlecode.common.*;


//COMMUNICATOR STUFF: WHAT ARRAY INDEX DOES WHAT
//IDX 0-3: Keep tracks of our HQ locations
//IDX 4-23: Assignment slots
//IDX 62-24: Used by carriers (and HQs at the beginning of round 1) to share well locations
//IDX 63: Keeps track of the possible symmetries the map may have
public class Communicator {



    public static int getHQIdx(RobotController rc, MapLocation hq) throws GameActionException {
        for(int i = 0; i < GameConstants.MAX_STARTING_HEADQUARTERS; i++) {
            int encoded = rc.readSharedArray(i) - 1;
            if(hq.x == encoded / 60 && hq.y == encoded % 60) return i;
        }
        System.out.println("No index found.");
        return 0;
    }

    public static void storeWellInfo(RobotController rc, int x, int y, ResourceType rt, boolean full) throws GameActionException {
        assert rc.canWriteSharedArray(0, 0);
        //if(alreadyRecorded(rc, new MapLocation(x, y))) return;
        for(int i = 62; i >= 24; i--) {
            if(rc.readSharedArray(i) == 0) {
                int encoded = Well.encodeWell(x, y, rt, full);
                if(rc.getRoundNum() >= 1) {
                    //if(rc.getTeam().equals(Team.A))
                        //System.out.println("Storing well at index " + i + ": " + x + " " + y + " " + rt.toString());
                }
                rc.writeSharedArray(i, encoded);
                return;
            }
            else {
                Well well = new Well(rc, i);
                if(well.wellX == x && well.wellY == y) {
                    rc.writeSharedArray(i, Well.encodeWell(x, y, rt, full));
                    return;
                }
            }
        }
        System.out.println("All well slots used, could not store well!");
    }

    public static boolean alreadyRecorded(RobotController rc, MapLocation loc) throws GameActionException {
        for(int i = 62; i >= 24; i--) {
            int wellState = rc.readSharedArray(i);
            Well well = new Well(wellState, i);
            if(wellState == 0) return false;
            if(well.wellX == loc.x && well.wellY == loc.y) return true;
        }
        return true;
    }
}
