package v4shell;

import battlecode.common.*;

import java.awt.*;
import java.util.*;

public class Headquarters {

    static final Direction[] directions = {
            Direction.NORTH,
            Direction.NORTHEAST,
            Direction.EAST,
            Direction.SOUTHEAST,
            Direction.SOUTH,
            Direction.SOUTHWEST,
            Direction.WEST,
            Direction.NORTHWEST,
    };

    static void detectWells(RobotController rc) throws GameActionException {
        for(WellInfo wi : rc.senseNearbyWells()) {
            MapLocation loc = wi.getMapLocation();
            ResourceType rt = wi.getResourceType();
            int count = rt == ResourceType.ADAMANTIUM ? 1 : 7;
            Communicator.storeWellInfo(rc, loc.x, loc.y, rt, count);
        }
    }

    static Direction towards(RobotController rc) throws GameActionException {
        if(rc.getRoundNum() < 3) {
            int hy = rc.getMapHeight()/2;
            int hx = rc.getMapWidth()/2;
            return rc.getLocation().directionTo(new MapLocation(hx, hy));
        }
        else return directions[rc.readSharedArray(63)-1];
    }

    static void spawnCarriers(RobotController rc) throws GameActionException {
        if (rc.getResourceAmount(ResourceType.ADAMANTIUM) < 50) return;

        Direction[] shuffled = directions.clone();
        Collections.shuffle(Arrays.asList(shuffled));

        for (Direction direction : shuffled) {
            MapLocation newLoc = rc.getLocation().add(direction).add(direction);
            if (rc.canBuildRobot(RobotType.CARRIER, newLoc)) {
                rc.writeSharedArray(62, rc.readSharedArray(62) + 1);
                rc.buildRobot(RobotType.CARRIER, newLoc);
                return;
            }
        }
    }

    static void spawnLaunchers(RobotController rc) throws GameActionException {
        if(rc.getResourceAmount(ResourceType.MANA) < 60) return;

        MapLocation spawnLoc = rc.getLocation().add(towards(rc)).add(towards(rc));
        for (int i = 0; i < directions.length + 1; i++) {
            MapLocation newLoc = spawnLoc;
            if (i != 0) newLoc = newLoc.add(directions[i - 1]);
            if (rc.canBuildRobot(RobotType.LAUNCHER, newLoc)) {
                rc.writeSharedArray(61, rc.readSharedArray(61) + 1);
                rc.buildRobot(RobotType.LAUNCHER, newLoc);
                break;
            }
        }
    }

    static void buildAnchors(RobotController rc) throws GameActionException {
        if(rc.canBuildAnchor(Anchor.STANDARD) && rc.getRoundNum() > 10) {
            rc.buildAnchor(Anchor.STANDARD);
        }
    }

    public static void run(RobotController rc) throws GameActionException {
        if(rc.getRoundNum() == 1) {
            Communicator.storeHQLoc(rc);
            detectWells(rc);
        }
        if(rc.getRoundNum() == 2 && rc.readSharedArray(63) == 0) MapStore.computeInitialSymmetry(rc);

        while(rc.isActionReady()) {
            buildAnchors(rc);
            spawnLaunchers(rc);
            spawnCarriers(rc);
        }
    }

}
