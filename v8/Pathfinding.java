package v8;

import battlecode.common.*;

import java.util.HashSet;
import java.util.Random;
import java.util.*;

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

    static boolean rotateRight = true;

    static void resetDistance() {
        bestDistance = 999999;
    }

    static int chebyshevDistance(MapLocation a, MapLocation b) {
        return a.distanceSquaredTo(b);
        //return Math.max(Math.abs(a.x - b.x), Math.abs(a.y - b.y));
    }

    static boolean towards(Direction d1, Direction d2) throws GameActionException {
        return (d1 == Direction.CENTER || d2 == Direction.CENTER || d1 == d2.rotateLeft() ||
                d1 == d2.rotateRight() || d1 == d2.rotateLeft().rotateLeft() || d1 == d2.rotateRight().rotateRight());
    }

    static boolean canMove(RobotController rc, Direction d) throws GameActionException {
        if(!rc.canMove(d)) return false;
        MapLocation m = rc.getLocation().add(d);
        if(rc.onTheMap(m)) {
            MapInfo info = rc.senseMapInfo(m);
            if(info.getCurrentDirection().opposite() == d) {
                return false;
            }
            if(info.getCurrentDirection() != Direction.CENTER && towards(info.getCurrentDirection().opposite(), d)) {
                return false;
            }
        }

        return true;
    }

    static boolean canMoveCardinal(RobotController rc, Direction d) throws GameActionException {
        if(!rc.canMove(d)) return false;
        boolean cardinal = false;
        for(Direction c : Direction.cardinalDirections()) {
            if(d.equals(c)) {
                cardinal = true;
                break;
            }
        }
        if(!cardinal) return false;

        MapLocation m = rc.getLocation().add(d);
        if(rc.onTheMap(m)) {
            MapInfo info = rc.senseMapInfo(m);
            if(info.getCurrentDirection().opposite() == d) {
                return false;
            }
            if(info.getCurrentDirection() != Direction.CENTER && towards(info.getCurrentDirection().opposite(), d)) {
                return false;
            }
        }

        return true;
    }

    static int turnsSinceRandomTargetChange = 0;
    public static boolean navigateRandomly(RobotController rc) throws GameActionException {
        turnsSinceRandomTargetChange++;
        if(target == null || rc.getLocation().distanceSquaredTo(target) < 5 ||
                turnsSinceRandomTargetChange > rc.getMapWidth() + rc.getMapHeight()) {
            int targetX = rng.nextInt(rc.getMapWidth());
            int targetY = rng.nextInt(rc.getMapHeight());
            target = new MapLocation(targetX, targetY);
            turnsSinceRandomTargetChange = 0;
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

    public static boolean navigateAwayFromCardinal(RobotController rc, MapLocation location) throws GameActionException {
        MapLocation myLoc = rc.getLocation();
        int targetX = myLoc.x * 2 - location.x;
        int targetY = myLoc.y * 2 - location.y;
        MapLocation targetLoc = new MapLocation(targetX, targetY);
        return navigateToLocationFuzzyCardinal(rc, targetLoc);
    }

    public static boolean navigateToLocationFuzzy(RobotController rc, MapLocation targetLoc) throws GameActionException {
        bestDistance = 999999;
        MapLocation myLoc = rc.getLocation();
        if (rc.canSenseLocation(targetLoc)) {
            if (!rc.sensePassability(targetLoc)) return false;
        }

        if (canMove(rc, myLoc.directionTo(targetLoc))) {
            rc.move(myLoc.directionTo(targetLoc));
        } else if (canMove(rc, myLoc.directionTo(targetLoc).rotateLeft())) {
            rc.move(myLoc.directionTo(targetLoc).rotateLeft());
        } else if (canMove(rc, myLoc.directionTo(targetLoc).rotateRight())) {
            rc.move(myLoc.directionTo(targetLoc).rotateRight());
        } else if (canMove(rc, myLoc.directionTo(targetLoc).rotateLeft().rotateLeft())) {
            rc.move(myLoc.directionTo(targetLoc).rotateLeft().rotateLeft());
        } else if (canMove(rc, myLoc.directionTo(targetLoc).rotateRight().rotateRight())) {
            rc.move(myLoc.directionTo(targetLoc).rotateRight().rotateRight());
        } else if (canMove(rc, myLoc.directionTo(targetLoc).rotateLeft().rotateLeft().rotateLeft())) {
            rc.move(myLoc.directionTo(targetLoc).rotateLeft().rotateLeft().rotateLeft());
        } else if (canMove(rc, myLoc.directionTo(targetLoc).rotateRight().rotateRight().rotateRight())) {
            rc.move(myLoc.directionTo(targetLoc).rotateRight().rotateRight().rotateRight());
        } else if (canMove(rc, myLoc.directionTo(targetLoc).rotateRight().rotateRight().rotateRight().rotateRight())) {
            rc.move(myLoc.directionTo(targetLoc).rotateRight().rotateRight().rotateRight().rotateRight());
        }
        return true;
    }

    public static boolean navigateToLocationFuzzyCardinal(RobotController rc, MapLocation targetLoc) throws GameActionException {
        bestDistance = 999999;
        MapLocation myLoc = rc.getLocation();
        if(rc.canSenseLocation(targetLoc)) {
            if(!rc.sensePassability(targetLoc)) return false;
        }

        if(canMoveCardinal(rc, myLoc.directionTo(targetLoc))) {
            rc.move(myLoc.directionTo(targetLoc));
        }
        else if(canMoveCardinal(rc, myLoc.directionTo(targetLoc).rotateLeft())){
            rc.move(myLoc.directionTo(targetLoc).rotateLeft());
        }
        else if(canMoveCardinal(rc, myLoc.directionTo(targetLoc).rotateRight())){
            rc.move(myLoc.directionTo(targetLoc).rotateRight());
        }
        else if(canMoveCardinal(rc, myLoc.directionTo(targetLoc).rotateLeft().rotateLeft())){
            rc.move(myLoc.directionTo(targetLoc).rotateLeft().rotateLeft());
        }
        else if(canMoveCardinal(rc, myLoc.directionTo(targetLoc).rotateRight().rotateRight())){
            rc.move(myLoc.directionTo(targetLoc).rotateRight().rotateRight());
        }
        else if(canMoveCardinal(rc, myLoc.directionTo(targetLoc).rotateLeft().rotateLeft().rotateLeft())){
            rc.move(myLoc.directionTo(targetLoc).rotateLeft().rotateLeft().rotateLeft());
        }
        else if(canMoveCardinal(rc, myLoc.directionTo(targetLoc).rotateRight().rotateRight().rotateRight())){
            rc.move(myLoc.directionTo(targetLoc).rotateRight().rotateRight().rotateRight());
        }
        else if(canMoveCardinal(rc, myLoc.directionTo(targetLoc).rotateRight().rotateRight().rotateRight().rotateRight())){
            rc.move(myLoc.directionTo(targetLoc).rotateRight().rotateRight().rotateRight().rotateRight());
        }
        return true;
    }

    public static boolean navigateToLocationBug(RobotController rc, MapLocation targetLoc) throws GameActionException {
        if(targetLoc != lastTarget) {
            bestDistance = 999999;
            lastDirection = null;
            lastTarget = targetLoc;
            rotateRight = true;
        }
        if(!rc.isMovementReady()) return true;
        if(bestDistance < 1) return true;

        if(rc.canSenseLocation(targetLoc) && !rc.sensePassability(targetLoc)) return false;

        rc.setIndicatorLine(rc.getLocation(), targetLoc, 255, 0, 0);
        String debug = "";

        while(rc.isMovementReady()) {
            boolean moved = false;
            MapLocation myLoc = rc.getLocation();
            bestDistance = Math.min(bestDistance, chebyshevDistance(myLoc, targetLoc));
            int bestDistanceNow = bestDistance;
            Direction bestDirection = Direction.CENTER;
            for (Direction direction : directions) {
                if(!rc.onTheMap(myLoc.add(direction))) {
                    continue;
                }
                MapLocation endLoc = myLoc.add(direction);
                Direction current = rc.senseMapInfo(endLoc).getCurrentDirection();

                if((rc.getType() == RobotType.LAUNCHER ||
                        (rc.getResourceAmount(ResourceType.MANA) +
                        rc.getResourceAmount(ResourceType.ADAMANTIUM)) > 30) &&
                        rc.canSenseLocation(endLoc.add(current)) &&
                        rc.sensePassability(endLoc.add(current)) &&
                        rc.senseRobotAtLocation(endLoc.add(current)) == null)
                {
                    endLoc = endLoc.add(current);
                }

                int distance = chebyshevDistance(endLoc, targetLoc);

                if(!canMove(rc, direction)) continue;

                //debug += direction + ": " + distance;

                if (distance < bestDistanceNow) {
                    bestDistanceNow = distance;
                    bestDirection = direction;
                }
            }
            if (bestDirection != Direction.CENTER) {
                if (canMove(rc, bestDirection) && rc.canMove(bestDirection)) {
                    rc.move(bestDirection);
                    lastDirection = null;
                    turnsWaited = 0;
                    moved = true;
                    rotateRight = true;
                    continue;
                }
            }
            if (lastDirection == null) {
                debug += "NULL";
                if(rotateRight) lastDirection = myLoc.directionTo(targetLoc).rotateRight().rotateRight();
                else lastDirection = myLoc.directionTo(targetLoc).rotateLeft().rotateLeft();
            }
            Direction direction = null;
            if (rotateRight) direction = lastDirection.rotateLeft().rotateLeft();
            else direction = lastDirection.rotateRight().rotateRight();
            for (int i = 0; i < 8; i++) {
                debug += lastDirection;
                debug += direction;
                MapLocation newLoc = myLoc.add(direction);

                if(!rc.onTheMap(newLoc)) {
                    //rotateRight = !rotateRight;
                    if(rotateRight)
                        direction = direction.rotateRight();
                    else direction = direction.rotateLeft();
                    continue;
                }
                if(!rc.canSenseLocation(newLoc) || !rc.sensePassability(newLoc)) {
                    if(rotateRight)
                        direction = direction.rotateRight();
                    else direction = direction.rotateLeft();
                    continue;
                }
                RobotInfo robotAtLoc = rc.senseRobotAtLocation(newLoc);
                /*if((robotAtLoc != null)) {
                    //rotateRight = !rotateRight;
                    if(rotateRight)
                        direction = direction.rotateRight();
                    else direction = direction.rotateLeft();
                    continue;
                }*/
                Direction current = rc.senseMapInfo(newLoc).getCurrentDirection();
                if ((!towards(current, direction))) {
                    if(rotateRight) {
                        rotateRight = false;
                        lastDirection = null;
                    }
                    direction = direction.rotateLeft();
                    continue;
                }


                if (canMove(rc, direction) && rc.canMove(direction)) {
                    rc.move(direction);
                    lastDirection = direction;
                    moved = true;
                    turnsWaited = 0;
                    break;
                } else if (rc.onTheMap(myLoc.add(direction)) && robotAtLoc != null &&
                        robotAtLoc.getType() == RobotType.HEADQUARTERS || turnsWaited > turnsToWait) {
                    if(rotateRight)
                        direction = direction.rotateRight();
                    else direction = direction.rotateLeft();
                } else {
                    turnsWaited += 1;
                    break;
                }
            }
            if(!moved) break;
        }

        rc.setIndicatorString(debug);
        return true;
    }

    public static boolean navigateToLocationBug2(RobotController rc, MapLocation targetLoc) throws GameActionException {
        int cooldown = (rc.getType() == RobotType.CARRIER ?
                        5 + 3*(rc.getResourceAmount(ResourceType.MANA) + rc.getResourceAmount(ResourceType.ADAMANTIUM))/8
                        : 10);
        int moves = Math.max((10-rc.getMovementCooldownTurns())/cooldown, 0);

        for(; moves > 0; moves--) {

        }


        return true;
    }
}
