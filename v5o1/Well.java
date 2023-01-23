package v5o1;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.ResourceType;
import battlecode.common.RobotController;

public class Well {
    int wellX;
    int wellY;
    ResourceType resourceType;
    int wellIdx;

    public Well(RobotController rc, int idx) throws GameActionException {
        int encoded = rc.readSharedArray(idx);
        resourceType = encoded % 2 == 1 ? ResourceType.MANA : ResourceType.ADAMANTIUM;
        wellY = (encoded /= 2) % 60;
        wellX = encoded / 60;
        wellIdx = idx;
    }

    public Well(int encoded, int idx) {
        resourceType = encoded % 2 == 1 ? ResourceType.MANA : ResourceType.ADAMANTIUM;
        wellY = (encoded /= 2) % 60;
        wellX = encoded / 60;
        wellIdx = idx;
    }

    public static int encodeWell(int wellX, int wellY, ResourceType resourceType) {
        return (wellX*60+wellY) * 2 + (resourceType == ResourceType.MANA ? 1 : 0);
    }

    public MapLocation getLoc() {
        return new MapLocation(wellX, wellY);
    }
}
