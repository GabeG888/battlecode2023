package Bot1;

import battlecode.common.*;

import java.awt.*;
import java.util.Random;
import java.util.Arrays;
import java.util.Comparator;


@SuppressWarnings("unused")
public strictfp class RobotPlayer {

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

    static  MapLocation myHQ;
    static MapLocation myWell;
    static ResourceType myResource;

    static MapLocation target;

    static int bestDistance = 999999;
    static Direction lastDirection;
    static MapLocation lastTarget;
    static int turnsWaited = 0;
    static int turnsToWait = 5;

    static int squadLeader;
    static MapLocation lastLeaderLocation;

    static Random rng = new Random();

    static void collectResources(RobotController rc) throws GameActionException {
        if(myResource == null) return;
        WellInfo[] wells = rc.senseNearbyWells(myResource);
        for(WellInfo well : wells) {
            if(rc.canCollectResource(well.getMapLocation(), well.getRate())) {
                rc.collectResource(well.getMapLocation(), well.getRate());
            }
        }
    }

    static void depositResources(RobotController rc) throws GameActionException {
        if(myResource == null) return;
        RobotInfo[] robots = rc.senseNearbyRobots(1000, rc.getTeam());
        for(RobotInfo robot : robots) {
            if(robot.getType() == RobotType.HEADQUARTERS &&
                    rc.canTransferResource(robot.getLocation(), myResource, rc.getResourceAmount(myResource))) {
                rc.transferResource(robot.getLocation(), myResource, rc.getResourceAmount(myResource));
            }
        }
    }

    static boolean navigateRandomly(RobotController rc) throws GameActionException {
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

    static boolean navigateAwayFrom(RobotController rc, MapLocation location) throws GameActionException {
        MapLocation myLoc = rc.getLocation();
        int targetX = myLoc.x * 2 - location.x;
        int targetY = myLoc.y * 2 - location.y;
        MapLocation targetLoc = new MapLocation(targetX, targetY);
        return navigateToLocationFuzzy(rc, targetLoc);
    }

    static boolean navigateToLocationFuzzy(RobotController rc, MapLocation targetLoc) throws GameActionException {
        bestDistance = 999999;
        MapLocation myLoc = rc.getLocation();
        if(rc.canSenseLocation(targetLoc)) {
            if(!rc.sensePassability(targetLoc)) return false;
        }

        if(rc.canMove(myLoc.directionTo(targetLoc))) {
            rc.move(myLoc.directionTo(targetLoc));
        }
        else if(rc.canMove(myLoc.directionTo(targetLoc).rotateLeft())){
            rc.move(myLoc.directionTo(targetLoc).rotateLeft());
        }
        else if(rc.canMove(myLoc.directionTo(targetLoc).rotateRight())){
            rc.move(myLoc.directionTo(targetLoc).rotateRight());
        }
        else if(rc.canMove(myLoc.directionTo(targetLoc).rotateLeft().rotateLeft())){
            rc.move(myLoc.directionTo(targetLoc).rotateLeft().rotateLeft());
        }
        else if(rc.canMove(myLoc.directionTo(targetLoc).rotateRight().rotateRight())){
            rc.move(myLoc.directionTo(targetLoc).rotateRight().rotateRight());
        }
        else if(rc.canMove(myLoc.directionTo(targetLoc).rotateLeft().rotateLeft().rotateLeft())){
            rc.move(myLoc.directionTo(targetLoc).rotateLeft().rotateLeft().rotateLeft());
        }
        else if(rc.canMove(myLoc.directionTo(targetLoc).rotateRight().rotateRight().rotateRight())){
            rc.move(myLoc.directionTo(targetLoc).rotateRight().rotateRight().rotateRight());
        }
        else if(rc.canMove(myLoc.directionTo(targetLoc).rotateRight().rotateRight().rotateRight().rotateRight())){
            rc.move(myLoc.directionTo(targetLoc).rotateRight().rotateRight().rotateRight().rotateRight());
        }
        return true;
    }

    static boolean navigateToLocationBug(RobotController rc, MapLocation targetLoc) throws GameActionException {
        if(targetLoc != lastTarget) {
            bestDistance = 999999;
            lastDirection = null;
            lastTarget = targetLoc;
        }
        if(!rc.isMovementReady()) return true;
        if(bestDistance < 1) return true;

        if(rc.canSenseLocation(targetLoc) && !rc.sensePassability(targetLoc)) return false;

        MapLocation myLoc = rc.getLocation();
        bestDistance = Math.min(bestDistance, myLoc.distanceSquaredTo(targetLoc));

        int bestDistanceNow = bestDistance;
        Direction bestDirection = Direction.CENTER;
        for(Direction direction : directions) {
            int distance = myLoc.add(direction).distanceSquaredTo(targetLoc);
            if(distance < bestDistanceNow && rc.canMove(direction)) {
                bestDistanceNow = distance;
                bestDirection = direction;
            }
        }
        if(bestDirection != Direction.CENTER) {
            if(rc.canMove(bestDirection)) {
                rc.move(bestDirection);
                lastDirection = bestDirection.rotateRight().rotateRight();
                turnsWaited = 0;
                return !rc.canSenseLocation(targetLoc) ||rc.sensePassability(targetLoc);
            }
        }

        if(lastDirection == null) lastDirection = myLoc.directionTo(targetLoc).rotateRight().rotateRight();
        Direction direction = lastDirection.rotateLeft().rotateLeft();
        for(int i = 0; i < 8; i++) {
            if(rc.onTheMap(myLoc.add(direction)) && !rc.sensePassability(myLoc.add(direction))) {
                direction = direction.rotateRight();
                continue;
            }
            if(rc.canMove(direction)) {
                rc.move(direction);
                lastDirection = direction;
                turnsWaited = 0;
                return true;
            }
            else if(rc.onTheMap(myLoc.add(direction)) && rc.senseRobotAtLocation(myLoc.add(direction)) != null &&
                    rc.senseRobotAtLocation(myLoc.add(direction)).getType() == RobotType.HEADQUARTERS ||
                    turnsWaited > turnsToWait) {
                bestDistance = 999999;
                direction = direction.rotateRight();
            }
            else {
                turnsWaited += 1;
                return true;
            }
        }
        return true;
    }

    static boolean surroundHQ(RobotController rc, RobotInfo enemy) throws GameActionException {
        MapLocation enemyLocation = enemy.getLocation();
        for(Direction direction : directions) {
            MapLocation loc = enemyLocation.add(direction);
            if(!rc.canSenseLocation(loc)) return navigateToLocationFuzzy(rc, loc);
            if(!rc.sensePassability(loc)) continue;
            if(rc.senseRobotAtLocation(loc) != null && rc.senseRobotAtLocation(loc).getTeam() == rc.getTeam()) continue;
            navigateToLocationFuzzy(rc, loc);
            squadLeader = enemy.getID();
            return true;
        }
        return false;
    }

    static boolean attackNearby(RobotController rc) throws GameActionException{
        RobotInfo[] enemies = rc.senseNearbyRobots(1000, rc.getTeam().opponent());
        MapLocation myLoc = rc.getLocation();

        Arrays.sort(enemies, Comparator.comparingInt(RobotInfo::getHealth));

        for(RobotInfo enemy : enemies) {
            if(rc.canAttack(enemy.getLocation())) {
                rc.attack(enemy.getLocation());
                return true;
            }
        }

        return false;
    }

    static boolean surroundHQs(RobotController rc) throws GameActionException{
        RobotInfo[] enemies = rc.senseNearbyRobots(1000, rc.getTeam().opponent());
        MapLocation myLoc = rc.getLocation();

        for(RobotInfo enemy : enemies) {
            if(enemy.getType() == RobotType.HEADQUARTERS) {
                if(!myLoc.isWithinDistanceSquared(enemy.getLocation(), 2)) {
                    if(surroundHQ(rc, enemy)) return true;
                }
                else return true;
            }
        }
        return false;
    }

    static boolean followLeader(RobotController rc) throws GameActionException{
        if(!rc.canSenseRobot(squadLeader)) return false;
        MapLocation leaderLocation = rc.senseRobot(squadLeader).getLocation();
        if(lastLeaderLocation == null) lastLeaderLocation = leaderLocation;
        if(rc.getLocation().distanceSquaredTo(leaderLocation) > 2)
        {
            lastLeaderLocation = leaderLocation;
            return navigateToLocationFuzzy(rc, leaderLocation);
        }
        else {
            lastLeaderLocation = leaderLocation;
            return navigateToLocationFuzzy(rc,
                    rc.getLocation().add(lastLeaderLocation.directionTo(leaderLocation)));
        }
    }

    static boolean maintainDistance(RobotController rc) throws GameActionException{
        RobotInfo[] enemies = rc.senseNearbyRobots(1000, rc.getTeam().opponent());
        MapLocation myLoc = rc.getLocation();

        RobotInfo closestEnemy = null;
        int closestDistance = 999;
        for(RobotInfo enemy : enemies) {
            if(enemy.getType() != RobotType.LAUNCHER) continue;
            int distance = myLoc.distanceSquaredTo(enemy.getLocation());
            if(distance < closestDistance) {
                closestEnemy = enemy;
                closestDistance = distance;
            }
        }

        if(closestEnemy == null) return false;

        return true;
    }

    static boolean makeSquad(RobotController rc) throws GameActionException{
        RobotInfo[] robots = rc.senseNearbyRobots(1000, rc.getTeam());
        int potentialSquadMembers = 0;
        int potentialLeader = rc.getID();
        for(RobotInfo robot : robots) {
            if(robot.getType() == RobotType.LAUNCHER && rc.getLocation().distanceSquaredTo(robot.getLocation()) < 3) {
                potentialSquadMembers++;
                if(robot.getID() < potentialLeader) potentialLeader = robot.getID();
            }
        }

        if(potentialSquadMembers > 1) {
            squadLeader = potentialLeader;
            return true;
        }
        else {
            if(rc.canSenseRobot(potentialLeader)) navigateToLocationFuzzy(rc, rc.senseRobot(potentialLeader).getLocation());
            return false;
        }
    }

    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        while (true) {

            try {
                switch (rc.getType()) {
                    case HEADQUARTERS:
                        runHeadquarters(rc);
                        break;
                    case CARRIER:
                        runCarrier(rc);
                        break;
                    case LAUNCHER:
                        runLauncher(rc);
                        break;
                    case BOOSTER:
                    case DESTABILIZER:
                    case AMPLIFIER:
                        break;
                }

            } catch (GameActionException e) {
                System.out.println(rc.getType() + " - Game Exception");
                e.printStackTrace();

            } catch (Exception e) {
                System.out.println(rc.getType() + " - Exception");
                e.printStackTrace();

            } finally {
                Clock.yield();
            }
        }
    }

    static void runHeadquarters(RobotController rc) throws GameActionException {
        if(rc.canBuildAnchor(Anchor.STANDARD) && rc.getRoundNum() > 10) {
            rc.buildAnchor(Anchor.STANDARD);
        }
        for(Direction direction : directions) {
            MapLocation newLoc = rc.getLocation().add(direction);
            if(rc.canBuildRobot(RobotType.CARRIER, newLoc)) {
                rc.buildRobot(RobotType.CARRIER, newLoc);
                rc.writeSharedArray(0, rc.readSharedArray(0) + 1);
            }
            if(rc.canBuildRobot(RobotType.LAUNCHER, newLoc)) {
                rc.buildRobot(RobotType.LAUNCHER, newLoc);
                rc.writeSharedArray(1, rc.readSharedArray(1) + 1);
            }
        }
    }

    static void runCarrier(RobotController rc) throws GameActionException {
        collectResources(rc);
        depositResources(rc);

        if(myHQ == null) {
            RobotInfo[] robots = rc.senseNearbyRobots(1000, rc.getTeam());
            for(RobotInfo robot : robots) {
                if(robot.getType() == RobotType.HEADQUARTERS){
                    myHQ = robot.getLocation();
                    break;
                }
            }
        }

        boolean moved = false;
        boolean attacked = false;
        RobotInfo[] enemies = rc.senseNearbyRobots(1000, rc.getTeam().opponent());
        for(RobotInfo enemy : enemies) {
            if(enemy.getType() != RobotType.HEADQUARTERS && !attacked && rc.canAttack(enemy.getLocation())) {
                rc.attack(enemy.getLocation());
                attacked = true;
            }
            if(enemy.getType() == RobotType.LAUNCHER) {
                navigateAwayFrom(rc, enemy.getLocation());
                moved = true;
            }
        }

        collectResources(rc);
        depositResources(rc);

        if(!moved) {
            if(myResource != null && rc.getResourceAmount(myResource) > 36) {
                navigateToLocationBug(rc, myHQ);
            }
            else {
                if(myWell == null) {
                    if(rc.readSharedArray(0) > rc.readSharedArray(1)) myResource = ResourceType.MANA;
                    else myResource = ResourceType.ADAMANTIUM;
                    WellInfo[] wells = rc.senseNearbyWells(myResource);
                    if(wells.length > 0) myWell = wells[rng.nextInt(wells.length)].getMapLocation();
                    else moved = navigateRandomly(rc);
                }
                if(myWell != null) {
                    moved = navigateToLocationBug(rc, myWell);
                }
            }
        }

        collectResources(rc);
        depositResources(rc);
    }

    static void runLauncher(RobotController rc) throws GameActionException {
        boolean moved = false;
        boolean attacked = false;

        if(myHQ == null) {
            RobotInfo[] robots = rc.senseNearbyRobots(1000, rc.getTeam());
            for(RobotInfo robot : robots) {
                if(robot.getType() == RobotType.HEADQUARTERS){
                    myHQ = robot.getLocation();
                    break;
                }
            }
        }

        if(!rc.canSenseRobot(squadLeader)) {
            if(!makeSquad(rc)) squadLeader = -1;
        }

        if(squadLeader == 0) {
            attackNearby(rc);
            makeSquad(rc);
            attackNearby(rc);
        }
        else if(squadLeader == -1) {
            attackNearby(rc);
            moved = surroundHQs(rc);
            attackNearby(rc);
            if(!moved) navigateRandomly(rc);
            attackNearby(rc);
        }
        else {
            if(squadLeader == rc.getID()) {
                attackNearby(rc);
                moved = surroundHQs(rc);
                attackNearby(rc);
                if(!moved) moved = maintainDistance(rc);
                attackNearby(rc);
                //navigateToLocationBug(rc, new MapLocation(rc.getMapWidth() - myHQ.x, rc.getMapHeight() - myHQ.y));
                if(!moved) navigateRandomly(rc);
                attackNearby(rc);
            }
            else {
                attackNearby(rc);
                moved = surroundHQs(rc);
                attackNearby(rc);
                if(!moved) moved = followLeader(rc);
                attackNearby(rc);
                if(!moved) maintainDistance(rc);
                attackNearby(rc);
            }
        }
    }
}
