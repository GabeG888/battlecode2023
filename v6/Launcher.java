package v6;

import battlecode.common.*;

import java.awt.*;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.*;

public class Launcher {

    static MapLocation myHQ = null;

    static int getAttackPriority(RobotInfo r) {
        RobotType type = r.getType();
        if(type == RobotType.DESTABILIZER) return 1;
        else if(type == RobotType.LAUNCHER) return 2;
        else if(type == RobotType.AMPLIFIER) return 3;
        else if(type == RobotType.CARRIER) return 4;
        return 5;
    }

    static Set<MapLocation> leftrightLocs = new HashSet<>();
    static Set<MapLocation> updownLocs = new HashSet<>();
    static Set<MapLocation> rotationalLocs = new HashSet<>();
    static int targetSymmetry = 0;
    static int possibleSymmetry = 7;
    static MapLocation target = null;
    static void initPotentialHQLocs(RobotController rc) throws GameActionException {
        //int symmetry = rc.readSharedArray(63);
        for(int i = 0 ; i < GameConstants.MAX_STARTING_HEADQUARTERS; i++) {
            int loc = rc.readSharedArray(i) - 1;
            if(loc == -1) return;
            int x = loc / 60, y = loc % 60;
            leftrightLocs.add(new MapLocation(rc.getMapWidth()-1 - x, y));
            updownLocs.add(new MapLocation(x, rc.getMapHeight()-1 - y));
            rotationalLocs.add(new MapLocation(rc.getMapWidth()-1 - x, rc.getMapHeight()-1 - y));
        }
    }


    static Set<MapLocation> skippedLocs = new HashSet<>();
    static void acquireTarget(RobotController rc) throws GameActionException {
        RobotInfo[] allies = rc.senseNearbyRobots(1000, rc.getTeam());
        int lowestID = rc.getID();
        RobotInfo leader = rc.senseRobot(lowestID);
        for(RobotInfo ally : allies) {
            if(ally.getID() < lowestID) {
                lowestID = ally.getID();
                leader = ally;
            }
        }

        MapLocation startLoc = leader.getLocation();

        int bestDist = 10000;

        if((possibleSymmetry & MapStore.LEFTRIGHT) > 0) {
            for(MapLocation loc : leftrightLocs) {
                if(skippedLocs.contains(loc)) continue;
                int dist = startLoc.distanceSquaredTo(loc);
                if(dist < bestDist) {
                    target = loc;
                    bestDist = dist;
                    targetSymmetry = MapStore.LEFTRIGHT;
                }
            }
        }
        if((possibleSymmetry & MapStore.UPDOWN) > 0) {
            for(MapLocation loc : updownLocs) {
                if(skippedLocs.contains(loc)) continue;
                int dist = startLoc.distanceSquaredTo(loc);
                if(dist < bestDist) {
                    target = loc;
                    bestDist = dist;
                    targetSymmetry = MapStore.UPDOWN;
                }
            }
        }
        if((possibleSymmetry & MapStore.ROTATIONAL) > 0) {
            for(MapLocation loc : rotationalLocs) {
                if(skippedLocs.contains(loc)) continue;
                int dist = startLoc.distanceSquaredTo(loc);
                if(dist < bestDist) {
                    target = loc;
                    bestDist = dist;
                    targetSymmetry = MapStore.ROTATIONAL;
                }
            }
        }
    }

    static boolean attackNearby(RobotController rc) throws GameActionException{
        RobotInfo[] enemies = rc.senseNearbyRobots(1000, rc.getTeam().opponent());

        Arrays.sort(enemies, Comparator.comparingInt(Launcher::getAttackPriority).thenComparingInt(RobotInfo::getHealth));

        for(RobotInfo enemy : enemies) {
            if(rc.canAttack(enemy.getLocation()) && enemy.getType() != RobotType.HEADQUARTERS) {
                rc.attack(enemy.getLocation());
                return true;
            }
        }
        return false;
    }

    static boolean maintainDistance(RobotController rc) throws GameActionException {
        RobotInfo[] enemies = rc.senseNearbyRobots(16, rc.getTeam().opponent());
        MapLocation myLoc = rc.getLocation();
        if(enemies.length < 2) return false;
        MapLocation closest = null;
        int dist = 10000;
        for(RobotInfo enemy : enemies) {
            if(enemy.getType() == RobotType.LAUNCHER){
                if(enemy.getLocation().distanceSquaredTo(myLoc) < dist) {
                    dist = enemy.getLocation().distanceSquaredTo(myLoc);
                    closest = enemy.getLocation();
                }
            }
        }
        if(closest != null) {
            return Pathfinding.navigateAwayFrom(rc, closest);
        }
        return false;
    }

    static boolean maintainDistanceAfterFiring(RobotController rc) throws GameActionException {
        RobotInfo[] enemies = rc.senseNearbyRobots(1000, rc.getTeam().opponent());
        MapLocation myLoc = rc.getLocation();

        MapLocation closest = null;
        int dist = 10000;
        for(RobotInfo enemy : enemies) {
            if(enemy.getType() == RobotType.LAUNCHER){
                if(enemy.getLocation().distanceSquaredTo(myLoc) < dist) {
                    dist = enemy.getLocation().distanceSquaredTo(myLoc);
                    closest = enemy.getLocation();
                }
            }
        }
        if(closest != null) {
            return Pathfinding.navigateAwayFrom(rc, closest);
        }
        return false;
    }

    static boolean surroundHQ(RobotController rc) throws GameActionException {
        if(target.distanceSquaredTo(rc.getLocation()) <= 18 && hasSeenTarget) {
            if(target.distanceSquaredTo(rc.getLocation()) <= 9)
                Pathfinding.navigateAwayFrom(rc, target);
            else {
                RobotInfo[] enemies = rc.senseNearbyRobots(1000, rc.getTeam().opponent());
                for(RobotInfo enemy : enemies) {
                    if(enemy.getType() == RobotType.LAUNCHER) return true;
                }

                Direction targetDirection = rc.getLocation().directionTo(target);
                for(int i = 0; i < 3; i++) {
                    MapLocation myLoc = rc.getLocation();
                    targetDirection = targetDirection.rotateRight();
                    int distance = myLoc.add(targetDirection).distanceSquaredTo(target);
                    if(distance < 21 && distance > 9 && rc.canMove(targetDirection)) rc.move(targetDirection);
                }
            }
            return true;
        }
        return false;
    }

    static int getWallsAround(RobotController rc, MapLocation loc) throws GameActionException {
        int walls = 0;
        for(Direction direction : Direction.allDirections()) {
            if(rc.canSenseLocation(loc.add(direction)) && !rc.sensePassability(loc.add(direction))) {
                walls++;
            }
        }
        return walls;
    }

    static void attackClouds(RobotController rc) throws GameActionException {
        MapLocation myLoc = rc.getLocation();
        MapLocation[] clouds = rc.senseNearbyCloudLocations();
        Arrays.sort(clouds, Comparator.comparingInt((MapLocation x) -> {
            try {
                return -getWallsAround(rc, x);
            } catch (GameActionException e) {
                e.printStackTrace();
                return 0;
            }
        }).thenComparingInt((MapLocation x) -> x.distanceSquaredTo(target)));
        for(MapLocation cloud : clouds) {
            if(myLoc.distanceSquaredTo(cloud) > 4 && rc.canAttack(cloud)) rc.attack(cloud);
        }

        if(!rc.senseCloud(myLoc)) return;
        MapLocation[] locs = rc.getAllLocationsWithinRadiusSquared(myLoc, 16);
        Arrays.sort(locs, Comparator.comparingInt((MapLocation x) -> x.distanceSquaredTo(target)));
        for(MapLocation loc : locs) {
            if(myLoc.distanceSquaredTo(loc) > 4 && rc.canAttack(loc)) rc.attack(loc);
        }
    }

    static boolean camping;
    static boolean hasSeenTarget;
    static void run(RobotController rc) throws GameActionException {
        RobotInfo[] enemies = rc.senseNearbyRobots(1000, rc.getTeam().opponent());
        RobotInfo[] allies = rc.senseNearbyRobots(1000, rc.getTeam());

        if(possibleSymmetry != rc.readSharedArray(63)) {
            possibleSymmetry &= rc.readSharedArray(63);
            acquireTarget(rc);
        }

        if(target == null) {
            initPotentialHQLocs(rc);
            acquireTarget(rc);
            hasSeenTarget = false;
        }

        if(target != null && rc.canSenseLocation(target)) {
            RobotInfo ri = rc.senseRobotAtLocation(target);
            if(ri == null || ri.getTeam() == rc.getTeam() || ri.getType() != RobotType.HEADQUARTERS) {
                possibleSymmetry &= ~targetSymmetry;
                //System.out.println("Old target: " + target);
                acquireTarget(rc);
                hasSeenTarget = false;
                //System.out.println("New target: " + target);
            }
            else {
                hasSeenTarget = true;
                camping = surroundHQ(rc);
            }
        }

        rc.setIndicatorString(target.toString() + " Symmetries (possible/target): " + possibleSymmetry+"/"+targetSymmetry);

        boolean attacked = attackNearby(rc);
        boolean moved = false;
        if(attacked) moved = maintainDistanceAfterFiring(rc);
        camping = surroundHQ(rc);
        if(camping) return;
        //if(rc.getRoundNum() >= 3) possibleSymmetry &= rc.readSharedArray(63);


        if(!attacked) {
            moved = maintainDistance(rc);
            if(!moved) Pathfinding.navigateToLocationBug(rc, target);
            attackNearby(rc);

            allies = rc.senseNearbyRobots(1000, rc.getTeam());
            int launchersSurrounding = 0;
            for(RobotInfo ally : allies) {
                if(ally.getType() == RobotType.LAUNCHER && ally.getLocation().distanceSquaredTo(target) <= 18) {
                    launchersSurrounding++;
                }
            }
            if(launchersSurrounding > 5 && skippedLocs.size() < leftrightLocs.size() - 1) {
                skippedLocs.add(target);
                acquireTarget(rc);
            }
        }

        attackClouds(rc);

        if(rc.canWriteSharedArray(0,0)) {
            rc.writeSharedArray(63, possibleSymmetry & rc.readSharedArray(63));
        }
    }
}
