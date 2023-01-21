package v4shell;

import battlecode.common.*;

import java.util.Random;

public class Pathfinding {

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

    static MapLocation target;
    static int bestDistance = 999999;
    static Direction lastDirection;
    static MapLocation lastTarget;
    static int turnsWaited = 0;
    static int turnsToWait = 5;
    static Random rng = new Random();

    static boolean canMove(RobotController rc, Direction d) throws GameActionException {
        if(!rc.canMove(d)) return false;
        MapLocation m = rc.getLocation().add(d);
        if(rc.onTheMap(m)) {
            MapInfo info = rc.senseMapInfo(m);
            if(info.getCurrentDirection().opposite() == d) {
                return false;
            }
        }

        return true;
    }

    public static boolean navigateRandomly(RobotController rc) throws GameActionException {
        if(target == null || rc.getLocation().distanceSquaredTo(target) < 5) {
            int targetX = rng.nextInt(rc.getMapWidth());
            int targetY = rng.nextInt(rc.getMapHeight());
            target = new MapLocation(targetX, targetY);
        }
        if(!navigateToLocationBug(rc, target)) {
            int targetX = rng.nextInt(rc.getMapWidth());
            int targetY = rng.nextInt(rc.getMapHeight());
            target = new MapLocation(targetX, targetY);
            return false;
        }
        return true;
    }

    public static boolean navigateAwayFrom(RobotController rc, MapLocation location) throws GameActionException {
        MapLocation myLoc = rc.getLocation();
        int targetX = myLoc.x * 2 - location.x;
        int targetY = myLoc.y * 2 - location.y;
        MapLocation targetLoc = new MapLocation(targetX, targetY);
        return navigateToLocationFuzzy(rc, targetLoc);
    }

    public static boolean navigateToLocationFuzzy(RobotController rc, MapLocation targetLoc) throws GameActionException {
        bestDistance = 999999;
        MapLocation myLoc = rc.getLocation();
        if(rc.canSenseLocation(targetLoc)) {
            if(!rc.sensePassability(targetLoc)) return false;
        }

        if(canMove(rc, myLoc.directionTo(targetLoc))) {
            rc.move(myLoc.directionTo(targetLoc));
        }
        else if(canMove(rc, myLoc.directionTo(targetLoc).rotateLeft())){
            rc.move(myLoc.directionTo(targetLoc).rotateLeft());
        }
        else if(canMove(rc, myLoc.directionTo(targetLoc).rotateRight())){
            rc.move(myLoc.directionTo(targetLoc).rotateRight());
        }
        else if(canMove(rc, myLoc.directionTo(targetLoc).rotateLeft().rotateLeft())){
            rc.move(myLoc.directionTo(targetLoc).rotateLeft().rotateLeft());
        }
        else if(canMove(rc, myLoc.directionTo(targetLoc).rotateRight().rotateRight())){
            rc.move(myLoc.directionTo(targetLoc).rotateRight().rotateRight());
        }
        else if(canMove(rc, myLoc.directionTo(targetLoc).rotateLeft().rotateLeft().rotateLeft())){
            rc.move(myLoc.directionTo(targetLoc).rotateLeft().rotateLeft().rotateLeft());
        }
        else if(canMove(rc, myLoc.directionTo(targetLoc).rotateRight().rotateRight().rotateRight())){
            rc.move(myLoc.directionTo(targetLoc).rotateRight().rotateRight().rotateRight());
        }
        else if(canMove(rc, myLoc.directionTo(targetLoc).rotateRight().rotateRight().rotateRight().rotateRight())){
            rc.move(myLoc.directionTo(targetLoc).rotateRight().rotateRight().rotateRight().rotateRight());
        }
        return true;
    }

    public static boolean navigateToLocationBug(RobotController rc, MapLocation targetLoc) throws GameActionException {
        if(targetLoc != lastTarget) {
            bestDistance = 999999;
            lastDirection = null;
            lastTarget = targetLoc;
        }
        if(!rc.isMovementReady()) return true;
        if(bestDistance < 1) return true;

        if(rc.canSenseLocation(targetLoc) && !rc.sensePassability(targetLoc)) return false;

        while(rc.isMovementReady()) {
            MapLocation myLoc = rc.getLocation();
            bestDistance = Math.min(bestDistance, myLoc.distanceSquaredTo(targetLoc));

            int bestDistanceNow = bestDistance;
            Direction bestDirection = Direction.CENTER;
            for (Direction direction : directions) {
                int distance = myLoc.add(direction).distanceSquaredTo(targetLoc);
                if (distance < bestDistanceNow && canMove(rc, direction)) {
                    bestDistanceNow = distance;
                    bestDirection = direction;
                }
            }
            if (bestDirection != Direction.CENTER) {
                if (canMove(rc, bestDirection)) {
                    rc.move(bestDirection);
                    lastDirection = bestDirection.rotateRight().rotateRight();
                    turnsWaited = 0;
                    return !rc.canSenseLocation(targetLoc) ||
                            rc.sensePassability(targetLoc);
                }
            }

            if (lastDirection == null) lastDirection = myLoc.directionTo(targetLoc).rotateRight().rotateRight();
            Direction direction = lastDirection.rotateLeft().rotateLeft();
            for (int i = 0; i < 8; i++) {
                if (rc.onTheMap(myLoc.add(direction)) && !rc.sensePassability(myLoc.add(direction))) {
                    direction = direction.rotateRight();
                    continue;
                }
                if (canMove(rc, direction)) {
                    rc.move(direction);
                    lastDirection = direction;
                    turnsWaited = 0;
                    return true;
                } else if (rc.onTheMap(myLoc.add(direction)) && rc.senseRobotAtLocation(myLoc.add(direction)) != null &&
                        rc.senseRobotAtLocation(myLoc.add(direction)).getType() == RobotType.HEADQUARTERS ||
                        turnsWaited > turnsToWait) {
                    bestDistance = 999999;
                    direction = direction.rotateRight();
                } else {
                    turnsWaited += 1;
                    return true;
                }
            }
        }
        return true;
    }

}
