package v6;

import battlecode.common.*;

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

        RobotInfo[] enemies = rc.senseNearbyRobots(1000, rc.getTeam().opponent());
        MapLocation[] enemyLocs = new MapLocation[enemies.length];
        for(int i = 0; i < enemies.length; i++) {
            enemyLocs[i] = enemies[i].getLocation();
        }
        while(rc.isActionReady()) {
            if (rc.getResourceAmount(ResourceType.ADAMANTIUM) < 50) return spawned;
            if(State.getState(rc) == State.COMPLETE_CONTROL && rc.getResourceAmount(ResourceType.ADAMANTIUM) < 150) return spawned;

            Direction[] shuffled = directions.clone();
            Collections.shuffle(Arrays.asList(shuffled));

            MapLocation bestLoc = null;
            int bestEnemies = 99;

            int spawnable = 0;
            int filled = 0;
            for(MapInfo mi : rc.senseNearbyMapInfos(9)) {
                RobotInfo there = rc.senseRobotAtLocation(mi.getMapLocation());
                if(mi.isPassable()) {
                    spawnable ++;
                    if(there != null && there.getTeam() == rc.getTeam() && there.getType() == RobotType.CARRIER) {
                        filled++;
                    }
                }
                if(rc.canBuildRobot(RobotType.CARRIER, mi.getMapLocation())) {
                    int enemiesNearby = 0;
                    for(MapLocation enemyLoc : enemyLocs) {
                        if(enemyLoc.distanceSquaredTo(mi.getMapLocation()) <= 16) enemiesNearby++;
                    }
                    if(enemiesNearby < bestEnemies) {
                        bestEnemies = enemiesNearby;
                        bestLoc = mi.getMapLocation();
                    }
                }
            }
            if(filled > spawnable / 2) return spawned;
            if(bestLoc != null) {
                rc.buildRobot(RobotType.CARRIER, bestLoc);
                spawned++;
            }
        }
        return spawned;
    }

    static void spawnLaunchers(RobotController rc) throws GameActionException {
        while(rc.isActionReady()) {
            if (rc.getResourceAmount(ResourceType.MANA) < 60) return;
            if(State.getState(rc) == State.COMPLETE_CONTROL && rc.getResourceAmount(ResourceType.MANA) < 160) return;
            MapLocation bestSpawn = null;
            int bestDist = 10000;
            for(MapInfo mi : rc.senseNearbyMapInfos(9)) {
                if(rc.canBuildRobot(RobotType.LAUNCHER, mi.getMapLocation())) {
                    int dist = mi.getMapLocation().distanceSquaredTo(target);
                    if(dist < bestDist) {
                        bestDist = dist;
                        bestSpawn = mi.getMapLocation();
                    }
                }
            }

            if(bestSpawn != null) rc.buildRobot(RobotType.LAUNCHER, bestSpawn);
            else return;
        }
    }

    static void acquireTarget(RobotController rc) throws GameActionException {
        int symmetry = MapStore.possibleSymmetry;
        int bestDist = 10000;

        for(int i = 0 ; i < GameConstants.MAX_STARTING_HEADQUARTERS; i++) {
            int encoded = rc.readSharedArray(i) - 1;
            if(encoded == -1) return;
            int x = encoded / 60, y = encoded % 60;

            if((symmetry & MapStore.LEFTRIGHT) > 0) {
                MapLocation loc = new MapLocation(rc.getMapWidth() - 1 - x, y);
                int dist = rc.getLocation().distanceSquaredTo(loc);
                if(dist < bestDist) {
                    target = loc;
                    bestDist = dist;
                }
            }
            if((symmetry & MapStore.UPDOWN) > 0) {
                MapLocation loc = new MapLocation(x, rc.getMapHeight() - 1 - y);
                int dist = rc.getLocation().distanceSquaredTo(loc);
                if(dist < bestDist) {
                    target = loc;
                    bestDist = dist;
                }
            }
            if((symmetry & MapStore.ROTATIONAL) > 0) {
                MapLocation loc = new MapLocation(rc.getMapWidth() - 1 - x, rc.getMapHeight() - 1 - y);
                int dist = rc.getLocation().distanceSquaredTo(loc);
                if(dist < bestDist) {
                    target = loc;
                    bestDist = dist;
                }
            }
        }
    }

    static MapLocation target = null;
    public static void run(RobotController rc) throws GameActionException {
        if(rc.getRoundNum() == 1) {
            storeHQLoc(rc);
            detectWells(rc);
            target = new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2);
        }
        if(rc.getRoundNum() == 2 && rc.readSharedArray(63) == 0) rc.writeSharedArray(63, 7);
        if(rc.getRoundNum() == 3)  {
            MapStore.computeInitialSymmetry(rc);
            acquireTarget(rc);
        }

        if(MapStore.possibleSymmetry != rc.readSharedArray(63)) {
            MapStore.possibleSymmetry = rc.readSharedArray(63);
            acquireTarget(rc);
        }

        rc.setIndicatorString(State.getState(rc).toString() + "; Symmetry: " + rc.readSharedArray(63));

        if(State.getState(rc) == State.COMPLETE_CONTROL) {
            if(rc.canBuildAnchor(Anchor.STANDARD)) {
                rc.buildAnchor(Anchor.STANDARD);
            }
            spawnLaunchers(rc);
            spawnCarriers(rc);
            return;
        }

        //Clear assignments list
        if(hqIdx == 0) {
            for(int i = 4; i <= 23; i++) {
                rc.writeSharedArray(i, 0);
            }
        }
        postAssignments(rc);

        //Spawn stuff
        spawnLaunchers(rc);
        int spawned = spawnCarriers(rc);
        //if(rc.getRoundNum() == 2) spawned += 2;

        //Queue assignments
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

        //if(rc.getRoundNum() == 100) rc.resign();
    }

    static int lastSpawn = 0;
    static int totalSpawns = 0;
    static int totalAdamMiners = 0;
    static Map<MapLocation, Integer> assigned = new HashMap<>();
    static ArrayList<Well> queuedAdamWells = new ArrayList<>();
    static ArrayList<Well> queuedManaWells = new ArrayList<>();

    static void postAssignments(RobotController rc) throws GameActionException {
        if(rc.getRoundNum() == 1) return;
        if(lastSpawn == 0) return;


        //int scouts = totalSpawns / 5;
        totalSpawns += lastSpawn;
        int adamMiners = totalSpawns / 4 - totalAdamMiners;

        //scouts = totalSpawns / 5 - scouts;
        //scouts/=2;
        //if(rc.getRoundNum() >= 100) scouts = 0;
        //if(lastSpawn - scouts - adamMiners <= 0) System.out.println("BAD: " + lastSpawn + " " + scouts + " " + adamMiners + " " + totalSpawns);
        if(queuedManaWells.size() > 0 && lastSpawn - adamMiners > 0) {
            for(int i = 4; i <= 23; i++) {
                if(rc.readSharedArray(i) == 0) {
                    Well well = queuedManaWells.get(0);
                    rc.writeSharedArray(i, Assignment.encodeAssignment(well.wellIdx, hqIdx, lastSpawn - adamMiners));
                    assigned.compute(well.getLoc(), (k, o) -> (o == null ? 1 : o + 1));
                    break;
                }
            }
        }
        if(queuedAdamWells.size() > 0) {
            for(int i = 4; i <= 23; i++) {
                if(rc.readSharedArray(i) == 0) {
                    Well well = queuedAdamWells.get(0);
                    rc.writeSharedArray(i, Assignment.encodeAssignment(well.wellIdx, hqIdx, adamMiners));
                    totalAdamMiners += adamMiners;
                    assigned.compute(well.getLoc(), (k, o) -> (o == null ? 1 : o + 1));
                    break;

                }
            }
        }
        //System.out.println("SPAWNED: " + lastSpawn + "; ADAM MINERS: " + adamMiners + "; TOTALS: " + totalSpawns + "/" + totalAdamMiners);

    }
}
