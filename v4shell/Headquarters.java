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

    static int hqIdx;

    static void storeHQLoc(RobotController rc) throws GameActionException {
        assert rc.getRoundNum() == 1;
        for(int i = 0; i < GameConstants.MAX_STARTING_HEADQUARTERS; i++) {
            if(rc.readSharedArray(i) == 0) {
                MapLocation loc = rc.getLocation();
                int encoded = loc.x * 60 + loc.y + 1;
                rc.writeSharedArray(i, encoded);
                hqIdx = i;
                return;
            }
        }
    }

    static void detectWells(RobotController rc) throws GameActionException {
        for(WellInfo wi : rc.senseNearbyWells()) {
            MapLocation loc = wi.getMapLocation();
            ResourceType rt = wi.getResourceType();
            Communicator.storeWellInfo(rc, loc.x, loc.y, rt);
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

    static int spawnCarriers(RobotController rc) throws GameActionException {
        int spawned = 0;
        while(rc.isActionReady()) {
            if (rc.getResourceAmount(ResourceType.ADAMANTIUM) < 50) return spawned;

            Direction[] shuffled = directions.clone();
            Collections.shuffle(Arrays.asList(shuffled));

            for (Direction direction : shuffled) {
                MapLocation newLoc = rc.getLocation().add(direction).add(direction);
                if (rc.canBuildRobot(RobotType.CARRIER, newLoc)) {
                    rc.writeSharedArray(62, rc.readSharedArray(62) + 1);
                    rc.buildRobot(RobotType.CARRIER, newLoc);
                    spawned++;
                    break;
                }
            }
        }
        return spawned;
    }

    static void spawnLaunchers(RobotController rc) throws GameActionException {
        while(rc.isActionReady()) {
            if (rc.getResourceAmount(ResourceType.MANA) < 60) return;

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
    }

    static void buildAnchors(RobotController rc) throws GameActionException {
        while(rc.isActionReady()) {
            if (rc.canBuildAnchor(Anchor.STANDARD) && rc.getRoundNum() > 10) {
                rc.buildAnchor(Anchor.STANDARD);
            }
        }
    }

    public static void run(RobotController rc) throws GameActionException {
        if(rc.getRoundNum() == 1) {
            storeHQLoc(rc);
            detectWells(rc);
        }
        if(rc.getRoundNum() == 2 && rc.readSharedArray(63) == 0) MapStore.computeInitialSymmetry(rc);

        if(hqIdx == 0) {
            for(int i = 4; i <= 23; i++) {
                rc.writeSharedArray(i, 0);
            }
        }
        postAssignments(rc);

        //spawnLaunchers(rc);
        int spawned = spawnCarriers(rc);
        if(rc.getRoundNum() == 2) spawned += 2;

        ArrayList<Well> manaWells = new ArrayList<>();
        ArrayList<Well> adamWells = new ArrayList<>();

        for(int i = 62; i >= 24; i--) {
            int encoded = rc.readSharedArray(i);
            if(encoded == 0) break;
            Well well = new Well(rc, i);

            if(well.resourceType == ResourceType.ADAMANTIUM)
                adamWells.add(well);
            else
                manaWells.add(well);
        }
        Collections.sort(adamWells,
                Comparator.comparingInt(x -> assigned.getOrDefault(x.getLoc(), 0)) );
        Collections.sort(manaWells,
                Comparator.comparingInt(x -> assigned.getOrDefault(x.getLoc(), 0)) );
        queuedAdamWells = adamWells;
        queuedManaWells = manaWells;
        lastSpawn = spawned;
    }

    static int lastSpawn = 0;
    static int totalSpawns = 0;
    static Map<MapLocation, Integer> assigned = new HashMap<>();
    static ArrayList<Well> queuedAdamWells = new ArrayList<>();
    static ArrayList<Well> queuedManaWells = new ArrayList<>();

    static void postAssignments(RobotController rc) throws GameActionException {
        if(rc.getRoundNum() == 1) return;
        int scouts = totalSpawns / 5;
        totalSpawns += lastSpawn;
        scouts = totalSpawns / 5 - scouts;

        if(queuedManaWells.size() > 0) {
            for(int i = 4; i <= 23; i++) {
                if(rc.readSharedArray(i) == 0) {
                    Well well = queuedManaWells.get(0);
                    rc.writeSharedArray(i, Assignment.encodeAssignment(well.wellIdx, hqIdx, lastSpawn - scouts));
                    assigned.compute(well.getLoc(), (k, o) -> (o == null ? 1 : o + 1));
                }
            }
        }
        else if(queuedAdamWells.size() > 0) {
            for(int i = 4; i <= 23; i++) {
                if(rc.readSharedArray(i) == 0) {
                    Well well = queuedAdamWells.get(0);
                    rc.writeSharedArray(i, Assignment.encodeAssignment(well.wellIdx, hqIdx, lastSpawn - scouts));
                    assigned.compute(well.getLoc(), (k, o) -> (o == null ? 1 : o + 1));
                }
            }
        }
    }
}
