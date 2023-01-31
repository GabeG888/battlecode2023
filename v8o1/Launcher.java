package v8o1;

import battlecode.common.*;

import java.util.*;

public class Launcher {

    static String debug = "";

    static void addToIndicatorString(String s) {
        debug += "; " + s;
    }

    static MapLocation myHQ = null;
    static int hqIdx;

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
            int loc = (rc.readSharedArray(i) & 0b0111_1111_1111_1111) - 1;
            if(loc == -1) return;
            int x = loc / 60, y = loc % 60;
            leftrightLocs.add(new MapLocation(rc.getMapWidth()-1 - x, y));
            updownLocs.add(new MapLocation(x, rc.getMapHeight()-1 - y));
            rotationalLocs.add(new MapLocation(rc.getMapWidth()-1 - x, rc.getMapHeight()-1 - y));
        }
    }


    static Set<MapLocation> skippedLocs = new HashSet<>();
    static void acquireTarget(RobotController rc) throws GameActionException {
        MapLocation startLoc = rc.getLocation();
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

    static boolean attackNearbyLaunchers(RobotController rc) throws GameActionException{
        RobotInfo[] enemies = rc.senseNearbyRobots(1000, rc.getTeam().opponent());

        Arrays.sort(enemies, Comparator.comparingInt(Launcher::getAttackPriority).thenComparingInt(RobotInfo::getHealth));

        for(RobotInfo enemy : enemies) {
            if(rc.canAttack(enemy.getLocation()) && enemy.getType() == RobotType.LAUNCHER) {
                rc.attack(enemy.getLocation());
                return true;
            }
        }
        return false;
    }

    static boolean maintainDistance(RobotController rc) throws GameActionException {
        RobotInfo[] enemies = rc.senseNearbyRobots(20, rc.getTeam().opponent());
        RobotInfo[] allies = rc.senseNearbyRobots(20, rc.getTeam());
        MapLocation myLoc = rc.getLocation();
        if(enemies.length == 0) return false;
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
            if(dist == 17) { // L shape 4x1
                for(Direction d : Direction.allDirections()) {
                    if(!rc.onTheMap(myLoc.add(d))) continue;
                    if(myLoc.add(d).add(rc.senseMapInfo(myLoc.add(d)).getCurrentDirection()).distanceSquaredTo(closest)
                            == 16 && rc.canMove(d)) {
                        rc.move(d);
                        Pathfinding.resetDistance();
                        return true;
                    }
                }
            }
            else { // dist must be 18; L shape 4x2
                for(Direction d : Direction.allDirections()) {
                    if(!rc.onTheMap(myLoc.add(d))) continue;
                    if(myLoc.add(d).add(rc.senseMapInfo(myLoc.add(d)).getCurrentDirection()).distanceSquaredTo(closest)
                            == 13 && rc.canMove(d)) {
                        rc.move(d);
                        Pathfinding.resetDistance();
                        return true;
                    }
                }
            }
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
            boolean moved = Pathfinding.navigateAwayFromCardinal(rc, closest);
            if(!moved) moved = Pathfinding.navigateAwayFrom(rc, closest);
            return moved;
        }
        return false;
    }

    static boolean surroundHQ(RobotController rc) throws GameActionException {
        if(target.distanceSquaredTo(rc.getLocation()) <= 18 && hasSeenTarget && rc.getRoundNum() % 2 == 0) {
            if(target.distanceSquaredTo(rc.getLocation()) <= 9) {
                Pathfinding.navigateAwayFromCardinal(rc, target);
                Pathfinding.navigateAwayFrom(rc, target);
            }
            else {
                RobotInfo[] enemies = rc.senseNearbyRobots(1000, rc.getTeam().opponent());
                for(RobotInfo enemy : enemies) {
                    if(enemy.getType() == RobotType.LAUNCHER) return true;
                }

                Direction targetDirection = rc.getLocation().directionTo(target);
                for(int i = 0; i < 3; i++) {
                    MapLocation myLoc = rc.getLocation();
                    if(rc.getRoundNum() % 100 < 50) targetDirection = targetDirection.rotateRight();
                    else targetDirection = targetDirection.rotateLeft();
                    MapLocation targetLoc = myLoc.add(targetDirection);
                    if(!rc.onTheMap(targetLoc)) continue;
                    int distance = targetLoc.add(rc.senseMapInfo(targetLoc)
                            .getCurrentDirection()).distanceSquaredTo(target);
                    if(distance < 21 && distance > 9 && rc.canMove(targetDirection)) {
                        Pathfinding.resetDistance();
                        rc.move(targetDirection);
                    }
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
        for(MapLocation cloud : clouds) {
            if(myLoc.distanceSquaredTo(cloud) > 4 && rc.canAttack(cloud)) rc.attack(cloud);
        }

        if(!rc.senseCloud(myLoc) || !rc.isActionReady()) return;

        MapLocation potentialLoc = new MapLocation(myLoc.x + 1, myLoc.y + 2);
        if(rc.onTheMap(potentialLoc) && rc.canAttack(potentialLoc)) {
            rc.attack(potentialLoc);
            return;
        }
        potentialLoc = new MapLocation(myLoc.x + 2, myLoc.y + 1);
        if(rc.onTheMap(potentialLoc) && rc.canAttack(potentialLoc)) {
            rc.attack(potentialLoc);
            return;
        }
        potentialLoc = new MapLocation(myLoc.x + 2, myLoc.y - 1);
        if(rc.onTheMap(potentialLoc) && rc.canAttack(potentialLoc)) {
            rc.attack(potentialLoc);
            return;
        }
        potentialLoc = new MapLocation(myLoc.x + 1, myLoc.y - 2);
        if(rc.onTheMap(potentialLoc) && rc.canAttack(potentialLoc)) {
            rc.attack(potentialLoc);
            return;
        }
        potentialLoc = new MapLocation(myLoc.x - 1, myLoc.y - 2);
        if(rc.onTheMap(potentialLoc) && rc.canAttack(potentialLoc)) {
            rc.attack(potentialLoc);
            return;
        }
        potentialLoc = new MapLocation(myLoc.x - 2, myLoc.y - 1);
        if(rc.onTheMap(potentialLoc) && rc.canAttack(potentialLoc)) {
            rc.attack(potentialLoc);
            return;
        }
        potentialLoc = new MapLocation(myLoc.x - 2, myLoc.y + 1);
        if(rc.onTheMap(potentialLoc) && rc.canAttack(potentialLoc)) {
            rc.attack(potentialLoc);
            return;
        }
        potentialLoc = new MapLocation(myLoc.x - 1, myLoc.y + 2);
        if(rc.onTheMap(potentialLoc) && rc.canAttack(potentialLoc)) {
            rc.attack(potentialLoc);
            return;
        }
        potentialLoc = new MapLocation(myLoc.x, myLoc.y + 2);
        if(rc.onTheMap(potentialLoc) && rc.canAttack(potentialLoc)) {
            rc.attack(potentialLoc);
            return;
        }
        potentialLoc = new MapLocation(myLoc.x + 2, myLoc.y);
        if(rc.onTheMap(potentialLoc) && rc.canAttack(potentialLoc)) {
            rc.attack(potentialLoc);
            return;
        }
        potentialLoc = new MapLocation(myLoc.x, myLoc.y - 2);
        if(rc.onTheMap(potentialLoc) && rc.canAttack(potentialLoc)) {
            rc.attack(potentialLoc);
            return;
        }
        potentialLoc = new MapLocation(myLoc.x - 2, myLoc.y);
        if(rc.onTheMap(potentialLoc) && rc.canAttack(potentialLoc)) {
            rc.attack(potentialLoc);
            return;
        }
    }

    static Map<MapLocation, ResourceType> wellsSeen = new HashMap<>();
    public static void recordWells(RobotController rc) {
        WellInfo[] wells = rc.senseNearbyWells();
        for(WellInfo well : wells) {
            MapLocation m = well.getMapLocation();
            wellsSeen.putIfAbsent(m, well.getResourceType());
        }
    }

    static boolean camping;
    static boolean hasSeenTarget;
    static boolean goingBackToHQ;
    static int defending = 0;
    static boolean defender = false;
    static boolean firstFour = false;
    static void run(RobotController rc) throws GameActionException {
        debug = "";

        if(rc.getRoundNum() == 2) firstFour = true;

        int enemyLaunchers = 0;
        RobotInfo[] enemies = rc.senseNearbyRobots(1000, rc.getTeam().opponent());
        for(RobotInfo enemy : enemies) {
            if(enemy.getType() == RobotType.LAUNCHER) enemyLaunchers++;
        }

        int allyLaunchers = 0;
        RobotInfo[] allies = rc.senseNearbyRobots(1000, rc.getTeam());
        for(RobotInfo ally : allies) {
            if(ally.getType() == RobotType.LAUNCHER) allyLaunchers++;
        }

        if (rc.canWriteSharedArray(0, 0)) {
            for (Map.Entry<MapLocation, ResourceType> entry : wellsSeen.entrySet()) {
                MapLocation loc = entry.getKey();
                ResourceType resource = entry.getValue();
                Communicator.storeWellInfo(rc, loc.x, loc.y, resource, false);
            }
        }

        if(myHQ == null) {
            int bestDist = 10000;
            for(int i = 0; i < 4; i++) {
                int encoded = (rc.readSharedArray(i) & 0b0111_1111_1111_1111) - 1;
                if(encoded == -1) continue;
                MapLocation hq = new MapLocation(encoded/60,encoded%60);
                if(hq.distanceSquaredTo(rc.getLocation()) < bestDist) {
                    myHQ = hq;
                    hqIdx = i;
                    bestDist = hq.distanceSquaredTo(rc.getLocation());
                }
            }
            if(enemies.length > 0 || (rc.readSharedArray(hqIdx) & 0b1000_0000_0000_0000) == 1) defender = true;
            if(myHQ == null) System.out.println("?????");
        }
        if(defender && ((rc.readSharedArray(hqIdx) & 0b1000_0000_0000_0000) == 0)) {
            defender = false;
        }


        if(possibleSymmetry != rc.readSharedArray(63)) {
            possibleSymmetry &= rc.readSharedArray(63);
            acquireTarget(rc);
        }

        if(target == null && !goingBackToHQ) {
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
                goingBackToHQ = true;
                hasSeenTarget = false;
                //System.out.println("New target: " + target);
            }
            else {
                hasSeenTarget = true;
                camping = surroundHQ(rc);
            }
        }
        if(goingBackToHQ && rc.getLocation().distanceSquaredTo(myHQ) <= 9) {
            goingBackToHQ = false;
            hasSeenTarget = false;
            acquireTarget(rc);
        }

        //rc.setIndicatorString(target.toString() + " Symmetries (possible/target): " + possibleSymmetry+"/"+targetSymmetry);

        boolean attacked = attackNearbyLaunchers(rc);
        boolean moved = false;
        if(attacked) {
            moved = maintainDistanceAfterFiring(rc);
            addToIndicatorString("Mantaining distance after firing");
        }

        camping = surroundHQ(rc);
        if(camping) {
            //rc.setIndicatorString("Camping");
            return;
        }
        //if(rc.getRoundNum() >= 3) possibleSymmetry &= rc.readSharedArray(63);

        if(!attacked) {
            moved = maintainDistance(rc);
            attackNearby(rc);
            if(moved) addToIndicatorString("Maintaining distance");
            else if(defender) {
                Pathfinding.navigateToLocationFuzzy(rc, myHQ);
                addToIndicatorString("Defending HQ");
            }
            else if(rc.getRoundNum() % 2 == 0) {
                if(goingBackToHQ) {
                    Pathfinding.navigateToLocationBug(rc, myHQ);
                    addToIndicatorString("Going to HQ");
                }
                else {
                    if(true) {
                        int minDist = 99999;
                        MapLocation leader = null;
                        int launchersNear = 0;
                        for(RobotInfo ally : rc.senseNearbyRobots(-1, rc.getTeam())) {
                            if(ally.getType() == RobotType.LAUNCHER) {
                                if(ally.getLocation().distanceSquaredTo(target) < minDist) {
                                    leader = ally.getLocation();
                                    minDist = ally.getLocation().distanceSquaredTo(target);
                                }
                                if(ally.getLocation().distanceSquaredTo(rc.getLocation()) <= 16) {
                                    launchersNear ++;
                                }
                            }
                        }
                        if(launchersNear < 2 && !rc.senseCloud(rc.getLocation())) {

                        }
                        else if(leader == null || rc.getLocation().distanceSquaredTo(target) < leader.distanceSquaredTo(target) || leader.distanceSquaredTo(rc.getLocation()) <= 9) {

                            Pathfinding.navigateToLocationBug(rc, target);
                            addToIndicatorString("Going to target");
                        }
                        else {
                            Pathfinding.navigateToLocationBug(rc, leader);
                            addToIndicatorString("Following leader");
                        }
                    }
                    else {
                        Pathfinding.navigateToLocationBug(rc, target);
                        addToIndicatorString("Going to target");
                    }
                }
            }
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

        //Check for symmetry
        for(int i = 62; i >= 24; i--) {
            if(rc.readSharedArray(i) == 0) break;
            Well well = new Well(rc, i);
            MapLocation loc = well.getLoc();
            int x = loc.x, y = loc.y;
            if((possibleSymmetry & MapStore.LEFTRIGHT) > 0) {
                if(rc.canSenseLocation(new MapLocation(rc.getMapWidth()-1 - x, y))) {
                    WellInfo wi = rc.senseWell(new MapLocation(rc.getMapWidth()-1 - x, y));
                    if(wi == null || wi.getResourceType() != well.resourceType) {
                        possibleSymmetry &= ~MapStore.LEFTRIGHT;
                        //System.out.println("LEFTRIGHT " + loc + " " + new MapLocation(rc.getMapWidth()-1 - x, y));
                    }
                }
            }
            if((possibleSymmetry & MapStore.UPDOWN) > 0) {
                if(rc.canSenseLocation(new MapLocation(x, rc.getMapHeight()-1 - y))) {
                    WellInfo wi = rc.senseWell(new MapLocation(x, rc.getMapHeight()-1 - y));
                    if(wi == null || wi.getResourceType() != well.resourceType) {
                        possibleSymmetry &= ~MapStore.UPDOWN;
                        //System.out.println("UPDOWN " + loc + " " + new MapLocation(x, rc.getMapHeight()-1 - y));

                    }
                }
            }
            if((possibleSymmetry & MapStore.ROTATIONAL) > 0) {
                if(rc.canSenseLocation(new MapLocation(rc.getMapWidth()-1 - x, rc.getMapHeight()-1 - y))) {
                    WellInfo wi = rc.senseWell(new MapLocation(rc.getMapWidth()-1 - x, rc.getMapHeight()-1 - y));
                    if(wi == null || wi.getResourceType() != well.resourceType) {
                        possibleSymmetry &= ~MapStore.ROTATIONAL;
                        //System.out.println("ROTATIONAL " + loc + " " + new MapLocation(rc.getMapWidth()-1 - x, rc.getMapHeight()-1 - y));

                    }
                }
            }
        }

        if(rc.canWriteSharedArray(0,0)) {
            rc.writeSharedArray(63, possibleSymmetry & rc.readSharedArray(63));
            goingBackToHQ = false;
        }

        if(target != null)
            addToIndicatorString(target.toString() + " Symmetries (possible/target): " + possibleSymmetry+"/"+targetSymmetry);
        if(target == null)
            addToIndicatorString("Symmetries (possible/target): " + possibleSymmetry+"/"+targetSymmetry);
        rc.setIndicatorString(debug);
    }
}
