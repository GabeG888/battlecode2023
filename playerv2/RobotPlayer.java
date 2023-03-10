package playerv2;

import battlecode.common.*;

import java.awt.*;
import java.util.*;


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
            if(distance < bestDistanceNow && canMove(rc, direction)) {
                bestDistanceNow = distance;
                bestDirection = direction;
            }
        }
        if(bestDirection != Direction.CENTER) {
            if(canMove(rc, bestDirection)) {
                rc.move(bestDirection);
                lastDirection = bestDirection.rotateRight().rotateRight();
                turnsWaited = 0;
                return !rc.canSenseLocation(targetLoc) ||
                        rc.sensePassability(targetLoc);
            }
        }

        if(lastDirection == null) lastDirection = myLoc.directionTo(targetLoc).rotateRight().rotateRight();
        Direction direction = lastDirection.rotateLeft().rotateLeft();
        for(int i = 0; i < 8; i++) {
            if(rc.onTheMap(myLoc.add(direction)) && !rc.sensePassability(myLoc.add(direction))) {
                direction = direction.rotateRight();
                continue;
            }
            if(canMove(rc, direction)) {
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

    enum State {
        CHILLING,
        LAUNCHER_SPAM,
        COMPLETE_CONTROL,
        ELIXIR_TIME
    }

    static State getState(RobotController rc) {
        if(rc.getRobotCount() >= rc.getMapHeight() * rc.getMapWidth() / 3) return State.COMPLETE_CONTROL;
        //if(rc.getRoundNum() > 300 && rc.getRobotCount() > rc.getMapWidth() * rc.getMapHeight() / 8) return State.ELIXIR_TIME;
        if(rc.getRoundNum() > 150) return State.LAUNCHER_SPAM;
        return State.CHILLING;
    }

    static Direction towards(RobotController rc) throws GameActionException {
        if(rc.getRoundNum() < 3) {
            int hy = rc.getMapHeight()/2;
            int hx = rc.getMapWidth()/2;
            return rc.getLocation().directionTo(new MapLocation(hx, hy));
        }
        else return directions[rc.readSharedArray(63)-1];
    }

    static void runHeadquarters(RobotController rc) throws GameActionException {

        MapLocation myLoc = rc.getLocation();
        if(rc.getRoundNum() == 1) {
            int i = 0;
            while(true) {
                if(rc.readSharedArray(i) == 0) {
                    rc.writeSharedArray(i, myLoc.x * 60 + myLoc.y);
                    break;
                }
                i++;
            }
            rc.setIndicatorString(myLoc.x * 60 + myLoc.y + "");
        }
        if(rc.getRoundNum() == 2 && rc.readSharedArray(63) == 0) {
            int i = 0;
            ArrayList<Integer> locs = new ArrayList<>();
            while(true) {
                int hqLoc = rc.readSharedArray(i);
                if(hqLoc == 0) {
                    break;
                }
                i++;
                locs.add(hqLoc);
            }
            int hy = rc.getMapHeight()/2;
            int hx = rc.getMapWidth()/2;
            boolean up = true, down = true, left = true, right = true;
            for(i = 0; i < locs.size(); i++) {
                int l = locs.get(i);
                int x = l / 60, y = l % 60;

                if(x < hx) left = false;
                else right = false;
                if(y < hy) down = false;
                else up = false;
            }
            Direction d = Direction.CENTER;
            if(up && left) d = Direction.NORTHWEST;
            else if(up && right) d = Direction.NORTHEAST;
            else if(down && left) d = Direction.SOUTHWEST;
            else if(down && right) d = Direction.SOUTHEAST;
            else if(up) d= Direction.NORTH;
            else if(down) d= Direction.SOUTH;
            else if(left) d= Direction.WEST;
            else if(right) d = Direction.EAST;
            else rc.setIndicatorDot(myLoc, 255, 255, 255);

            rc.writeSharedArray(63, Arrays.asList(directions).indexOf(d) + 1);
        }
        else {
            rc.setIndicatorString(getState(rc) + "");
        }
        if(rc.canBuildAnchor(Anchor.STANDARD) && rc.getRoundNum() > 10) {
            rc.buildAnchor(Anchor.STANDARD);
        }
        switch(getState(rc)) {
            case CHILLING:
                if (rc.getResourceAmount(ResourceType.ADAMANTIUM) >= 50) {
                    Direction[] shuffled = directions.clone();
                    Collections.shuffle(Arrays.asList(shuffled));

                    for (Direction direction : shuffled) {
                        MapLocation newLoc = rc.getLocation().add(direction).add(direction);
                        //rc.setIndicatorDot(newLoc, 255,0,0);

                        if (rc.canBuildRobot(RobotType.CARRIER, newLoc)) {
                            rc.writeSharedArray(62, rc.readSharedArray(62) + 1);
                            rc.buildRobot(RobotType.CARRIER, newLoc);
                            return;
                        }
                    }
                }

                Direction direction = towards(rc);
                MapLocation spawnLoc = rc.getLocation().add(direction).add(direction);
                for (int i = 0; i < directions.length + 1; i++) {
                    MapLocation newLoc = spawnLoc;
                    if (i != 0) newLoc = newLoc.add(directions[i - 1]);
                    //rc.setIndicatorDot(newLoc, 0,255,0);
                    if (rc.canBuildRobot(RobotType.LAUNCHER, newLoc)) {
                        rc.writeSharedArray(61, rc.readSharedArray(61) + 1);
                        rc.buildRobot(RobotType.LAUNCHER, newLoc);
                        break;
                    }
                }
                break;
            case LAUNCHER_SPAM:
                direction = towards(rc);
                spawnLoc = rc.getLocation().add(direction).add(direction);
                for (int i = 0; i < directions.length + 1; i++) {
                    MapLocation newLoc = spawnLoc;
                    if (i != 0) newLoc = newLoc.add(directions[i - 1]);
                    //rc.setIndicatorDot(newLoc, 0,255,0);
                    if (rc.canBuildRobot(RobotType.LAUNCHER, newLoc)) {
                        rc.writeSharedArray(61, 0);
                        rc.buildRobot(RobotType.LAUNCHER, newLoc);
                        break;
                    }
                }

                if (rc.getResourceAmount(ResourceType.ADAMANTIUM) >= 50) {
                    Direction[] shuffled = directions.clone();
                    Collections.shuffle(Arrays.asList(shuffled));

                    for (Direction d : shuffled) {
                        MapLocation newLoc = rc.getLocation().add(d).add(d);
                        //rc.setIndicatorDot(newLoc, 255,0,0);

                        if (rc.canBuildRobot(RobotType.CARRIER, newLoc)) {
                            rc.writeSharedArray(62, rc.readSharedArray(62) + 1);
                            rc.buildRobot(RobotType.CARRIER, newLoc);
                            return;
                        }
                    }
                }
                break;
            case COMPLETE_CONTROL:
                if(rc.canBuildAnchor(Anchor.STANDARD)) {
                    rc.buildAnchor(Anchor.STANDARD);
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

        if(getState(rc) == State.COMPLETE_CONTROL) {
            if(rc.getAnchor() != null) {
                int islan = rc.senseIsland(rc.getLocation());
                if(islan != -1 && rc.senseAnchor(islan) == null) {
                    rc.placeAnchor();
                    return;
                }
                int[] islands = rc.senseNearbyIslands();
                for(int island : islands) {
                    if(rc.senseAnchor(island) != null) continue;
                    MapLocation[] locs = rc.senseNearbyIslandLocations(island);
                    navigateToLocationBug(rc, locs[0]);
                }
            }
            RobotInfo[] allies = rc.senseNearbyRobots(1000, rc.getTeam());
            for(RobotInfo ally : allies) {
                if(ally.getType() == RobotType.HEADQUARTERS) {
                    if(rc.canTakeAnchor(ally.getLocation(), Anchor.STANDARD)) {
                        rc.takeAnchor(ally.getLocation(), Anchor.STANDARD);
                    }
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
                    if(rc.readSharedArray(62) > rc.readSharedArray(61)) myResource = ResourceType.MANA;
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

        //rc.setIndicatorString(myHQ.toString() + " " + myWell.toString());
    }

    static int getAttackPriority(RobotInfo r) {
        RobotType type = r.getType();
        if(type == RobotType.DESTABILIZER) return 1;
        else if(type == RobotType.LAUNCHER) return 2;
        else if(type == RobotType.AMPLIFIER) return 3;
        else if(type == RobotType.CARRIER) return 4;
        return 5;
    }
    static boolean attackNearby(RobotController rc) throws GameActionException{
        RobotInfo[] enemies = rc.senseNearbyRobots(1000, rc.getTeam().opponent());
        MapLocation myLoc = rc.getLocation();

        Arrays.sort(enemies, Comparator.comparingInt(RobotPlayer::getAttackPriority).thenComparingInt(RobotInfo::getHealth));

        for(RobotInfo enemy : enemies) {
            if(rc.canAttack(enemy.getLocation())) {
                rc.attack(enemy.getLocation());
                return true;
            }
        }
        return false;
    }
    static int hqIdx = 0;
    static int hqs = 0;
    static MapLocation enemyHQ;
    static void initEnemyLoc(RobotController rc, Direction d) throws GameActionException {
        int x = myHQ.x, y = myHQ.y;
        int w = rc.getMapWidth(), h = rc.getMapHeight();

        /*int mX = rc.getLocation().x, mY = rc.getLocation().y;
        if(d == Direction.EAST) { x = w-1; }
        else if(d == Direction.WEST) { x = 0; }
        else if(d == Direction.NORTH) { y = h-1; }
        else if (d == Direction.SOUTH) {y = 0; }
        else if(d == Direction.SOUTHWEST) { y = 0; x = 0;}
        else if(d==Direction.NORTHEAST) { y = h-1; x = w-1;}
        else if (d == Direction.NORTHWEST) { x = 0; y = h-1;}
        else if(d == Direction.SOUTHEAST) { y = 0; x = w-1; }
        enemyHQ = new MapLocation(x, y);*/
        if(getState(rc) == State.LAUNCHER_SPAM) {
            x = w/2; y = h/2;
        }
        else if(mode == 0) {
            if(d == Direction.EAST || d == Direction.WEST) {
                x = w-x;
            }
            else if(d == Direction.NORTH || d == Direction.SOUTH) {
                y = h-y;
            }
            else { x = w-x; y = h-y; }
        }
        else {
            x = w-x; y = h-y;
        }
        enemyHQ = new MapLocation(x, y);
    }
    static boolean camping = false;
    static int mode = 0;
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
            while(hqs < 4) {
                if(rc.readSharedArray(hqs) == 0) {
                    hqs++;
                    break;
                }
                int loc = rc.readSharedArray(hqs);
                int x = loc / 60, y = loc % 60;
                if(x == myHQ.x && y == myHQ.y) {
                    hqIdx = hqs;
                }
                hqs++;
            }
            mode = rc.readSharedArray(61) % 2;
            initEnemyLoc(rc, towards(rc));
        }
        attacked = attackNearby(rc);
        RobotInfo[] enemies = rc.senseNearbyRobots(1000, rc.getTeam().opponent());
        if(camping) return;
        for(RobotInfo enemy : enemies) {
            if(enemy.getType() == RobotType.HEADQUARTERS) {
                int campers = 0;
                for(Direction d : Direction.allDirections()) {
                    if(!rc.canSenseRobotAtLocation(enemy.getLocation().add(d))) continue;
                    RobotInfo ri = rc.senseRobotAtLocation(enemy.getLocation().add(d));
                    if (ri != null && ri.getTeam() == rc.getTeam() && ri.getType() == RobotType.LAUNCHER) {
                        campers ++;
                    }
                }
                if(!camping && campers < 4 && rc.getLocation().distanceSquaredTo(enemy.getLocation()) <= 2) {
                    camping = true;
                    return;
                }
                if(campers < 3) {
                    moved = navigateToLocationBug(rc, enemy.getLocation());
                }
                else {
                    mode = 1 - mode;
                    initEnemyLoc(rc, towards(rc));
                    moved = navigateToLocationBug(rc, enemyHQ);
                    return;
                }
            }
        }
        /*if(!rc.canSenseRobot(squadLeader)) {
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
            }
        }*/
        if(enemyHQ != null) rc.setIndicatorString(enemyHQ.toString());
        if (!attacked /*squadLeader == rc.getID()*/) {
            if(rc.getLocation().distanceSquaredTo(enemyHQ) <= 4) {
                mode = 1 - mode;
                initEnemyLoc(rc, towards(rc));
                rc.setIndicatorString("CHANGE! " + enemyHQ.toString());
            }

            if(rng.nextInt(5) == 0) {
                //enemyHQ = enemyHQ.add(enemyHQ.directionTo(new MapLocation(rc.getMapWidth()/2, rc.getMapHeight()/2)));
            }
            Direction d = towards(rc);
            Direction[] sides = new Direction[] { d, d.rotateLeft(), d.rotateRight()};
            for(Direction ds : sides) {
                if(rc.onTheMap(rc.getLocation().add(ds)) && rc.senseMapInfo(rc.getLocation().add(ds)).hasCloud()) {
                    if(rc.canMove(ds)) rc.move(ds);
                    return;
                }
            }
            if(!moved) navigateToLocationBug(rc, enemyHQ);
        }
        /*else if (rc.canSenseRobot(squadLeader)) {
            navigateToLocationFuzzy(rc, rc.senseRobot(squadLeader).getLocation());
        }*/
    }
}