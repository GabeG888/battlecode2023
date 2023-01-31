package v8o1;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.ResourceType;
import battlecode.common.RobotController;

public class Well {
    int wellX;
    int wellY;
    ResourceType resourceType;
    int wellIdx;
    boolean full;

    public Well(RobotController rc, int idx) throws GameActionException {
        int encoded = rc.readSharedArray(idx);
        full = encoded % 2 == 1;
        resourceType = (encoded /= 2) % 2 == 1 ? ResourceType.MANA : ResourceType.ADAMANTIUM;
        wellY = (encoded /= 2) % 60;
        wellX = encoded / 60;
        wellIdx = idx;
    }

    public Well(int encoded, int idx) {
        full = encoded % 2 == 1;
        resourceType = (encoded /= 2) % 2 == 1 ? ResourceType.MANA : ResourceType.ADAMANTIUM;
        wellY = (encoded /= 2) % 60;
        wellX = encoded / 60;
        wellIdx = idx;
    }

    public static int encodeWell(int wellX, int wellY, ResourceType resourceType, boolean full) {
        return ((wellX*60+wellY) * 2 + (resourceType == ResourceType.MANA ? 1 : 0)) * 2 + (full ? 1 : 0);
    }

    public MapLocation getLoc() {
        return new MapLocation(wellX, wellY);
    }
}
