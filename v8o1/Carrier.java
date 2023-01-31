package v8o1;

import battlecode.common.*;

import java.util.HashMap;
import java.util.Map;

public class Carrier {
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
    static int hqIdx = 0;
    static MapLocation myHQ = null;
    static MapLocation myWell = null;
    static ResourceType myResource;
    static boolean scout = false;
    static Map<MapLocation, ResourceType> wellsSeen = new HashMap<>();
    static boolean initialAdam = false;
    static boolean myWellFull = false;
    static int possibleSymmetry = 7;

    static int turnsAlive = 0;
    static boolean goingBackToHQ;

    static int goingBackTurns = 0;
    static MapLocation lastLocation;
    static int turnsStuck = 0;

    public static void run(RobotController rc) throws GameActionException {
        if(!goingBackToHQ) initHQ(rc);
        if(myWell == null && !scout && rc.canWriteSharedArray(0, 0)) receiveAssignment(rc);

        if(scout && turnsAlive < 3 && myHQ != null) {
            Pathfinding.navigateAwayFrom(rc, myHQ);
            turnsAlive++;
        }
        if(possibleSymmetry != rc.readSharedArray(63)) {
            possibleSymmetry &= rc.readSharedArray(63);
        }
        depositResources(rc);
        if(anchorStuff(rc)) return;

        collectResources(rc);

        recordWells(rc);


        boolean moved = false;
        RobotInfo[] enemies = rc.senseNearbyRobots(1000, rc.getTeam().opponent());
        for(RobotInfo enemy : enemies) {
            if(enemy.getType() == RobotType.LAUNCHER && rc.canAttack(enemy.getLocation())){// &&
                    //(rc.getHealth() <= 20 || myResource == ResourceType.ADAMANTIUM)) {
                rc.attack(enemy.getLocation());
            }
            if(enemy.getType() == RobotType.LAUNCHER) {

                int bestDist = 999999;
                for(int i = 0; i < 4; i++) {
                    int encoded = (rc.readSharedArray(i) & 0b0111_1111_1111_1111) - 1;
                    if(encoded == -1) continue;
                    MapLocation hq = new MapLocation((encoded)/60,(encoded)%60);
                    if(hq.distanceSquaredTo(rc.getLocation()) < bestDist && !getSurrounded(rc, i)) {
                        myHQ = hq;
                        bestDist = hq.distanceSquaredTo(rc.getLocation());
                    }
                }
                goingBackTurns = 10;
                Pathfinding.navigateToLocationBug(rc, myHQ);
                moved = true;
            }
            if(enemy.getType() == RobotType.HEADQUARTERS && rc.getLocation().distanceSquaredTo(enemy.getLocation()) <= 9) {
                Pathfinding.navigateRandomly(rc);
            }
        }

        collectResources(rc);
        depositResources(rc);
        recordWells(rc);

        if (myHQ != null && (myResource != null && rc.getResourceAmount(myResource) > 38) || goingBackToHQ || goingBackTurns > 0) {
            if(goingBackTurns > 0) goingBackTurns--;
            Pathfinding.navigateToLocationBug(rc, myHQ);
            //if(myWell != null)  rc.setIndicatorString(myWell.toString() + " " + myResource.toString() + "; FULL:  " + myWellFull + "; Symmetry: " + possibleSymmetry);
            return;
        }
        else if(myWell != null) {
            moved = navigateToWell(rc);
        }

        if(myResource == null || myResource == ResourceType.MANA) {
            WellInfo[] manaWells = rc.senseNearbyWells(ResourceType.MANA);
            for (WellInfo manaWell : manaWells) {
                //if (Communicator.alreadyRecorded(rc, manaWell.getMapLocation())) continue;
                if(myWell != null && manaWell.getMapLocation().distanceSquaredTo(myHQ) > myWell.distanceSquaredTo(myHQ)) continue;
                if(wellFull(rc, manaWell.getMapLocation())) continue;

                myResource = ResourceType.MANA;
                myWell = manaWell.getMapLocation();
                scout = false;
            }
        }
        if(initialAdam) {
            WellInfo[] adamWells = rc.senseNearbyWells(ResourceType.ADAMANTIUM);
            for (WellInfo adamWell : adamWells) {
                //if (Communicator.alreadyRecorded(rc, adamWell.getMapLocation())) continue;
                if(myWell != null && adamWell.getMapLocation().distanceSquaredTo(myHQ) > myWell.distanceSquaredTo(myHQ)) continue;
                if(wellFull(rc, adamWell.getMapLocation())) continue;
                myResource = ResourceType.ADAMANTIUM;
                myWell = adamWell.getMapLocation();
                scout = false;
            }
        }

        if(myWell != null) {
            moved = navigateToWell(rc);
            collectResources(rc);

        }
        if(myWell == null) {
            moved = Pathfinding.navigateRandomly(rc);
        }

        recordWells(rc);
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
        }

        /*if(rc.getLocation() == lastLocation) {
            if(turnsStuck >= 10 && (myResource == null || rc.getResourceAmount(myResource) == 0)) {
                rc.disintegrate();
            }
            else turnsStuck ++;
        }
        else {
            turnsStuck = 0;
        }*/

        lastLocation = rc.getLocation();

        //if(moved)
        //    MapStore.updateMap(rc);
        if(myWell != null)  rc.setIndicatorString(myWell.toString() + " " + myResource.toString() + "; FULL:  " + myWellFull + "; Symmetry: " + possibleSymmetry);
        else rc.setIndicatorString("Symmetry: " + possibleSymmetry);
    }

    static boolean getSurrounded(RobotController rc, int index) throws GameActionException {
        return (rc.readSharedArray(index) & 0b1000_0000_0000_0000) > 0;
    }

    static boolean wellFull(RobotController rc, MapLocation well) throws GameActionException {
        boolean full = true;
        if(!rc.canSenseLocation(well)) return false;
        for(Direction d : directions) {
            MapLocation spot = well.add(d);
            //if(!rc.canSenseLocation(spot)) full = false;
            if(rc.canSenseLocation(spot) && rc.sensePassability(spot)) {
                RobotInfo robotAtSpot = rc.senseRobotAtLocation(spot);
                if(robotAtSpot == null || robotAtSpot.getType() != RobotType.CARRIER || robotAtSpot.getTeam() == rc.getTeam().opponent())
                    full = false;
            }
        }
        return full;
    }

    static boolean navigateToWell(RobotController rc) throws GameActionException {
        if(myWell.distanceSquaredTo(rc.getLocation()) > 2) {
            if(rc.canSenseLocation(myWell)) {
                boolean full = true;
                MapLocation bestSpot = null;
                int bestDist = 10000;
                for(Direction d : directions) {
                    MapLocation spot = myWell.add(d);
                    if(!rc.canSenseLocation(spot) && rc.onTheMap(spot)) full = false;
                    if(rc.canSenseLocation(spot) && rc.sensePassability(spot) && rc.senseRobotAtLocation(spot) == null) {
                        full = false;
                        if(spot.distanceSquaredTo(rc.getLocation()) < bestDist) {
                            bestSpot = spot;
                            bestDist = spot.distanceSquaredTo(rc.getLocation());
                        }
                    }
                }
                if(full) {
                    myWellFull = false;
                    myWell = null;
                    return false;
                }
                if(bestSpot != null) {
                   // rc.setIndicatorString(bestSpot.toString());
                    if(bestDist <= 2 && rc.canMove(rc.getLocation().directionTo(bestSpot))) {
                        rc.move(rc.getLocation().directionTo(bestSpot));
                        return true;
                    }
                }
                return Pathfinding.navigateToLocationBug(rc, myWell);
            }
            return Pathfinding.navigateToLocationBug(rc, myWell);
        }
        else if(myWell.distanceSquaredTo(rc.getLocation()) <= 2) {
            if(rc.canMove(rc.getLocation().directionTo(myWell))) {
                rc.move(rc.getLocation().directionTo(myWell));
                return true;
            }
        }
        return false;
    }

    static void receiveAssignment(RobotController rc) throws GameActionException {
        for(int i = 4; i <= 23; i++) {
            int wellState = rc.readSharedArray(i);
            Assignment a = new Assignment(wellState);
            if(a.num > 0 && a.hqIdx == hqIdx) {
                a.num--;
                rc.writeSharedArray(i, a.encodeAssignment());
                Well well = new Well(rc, a.wellIdx);
                myWell = well.getLoc();
                myResource = well.resourceType;
                if(myResource == ResourceType.ADAMANTIUM) initialAdam = true;
                //if(rc.getTeam().equals(Team.A))
                //System.out.println("receiving assignment: "+  well.wellIdx + " " + wellState);
                return;
            }
        }
        scout = true;
    }

    static void initHQ(RobotController rc) throws GameActionException {
        if(rc.getRoundNum() == 1) return;
        for(RobotInfo robot : rc.senseNearbyRobots(1000, rc.getTeam())) {
            if(robot.getType() == RobotType.HEADQUARTERS) {
                myHQ = robot.getLocation();
                hqIdx = Communicator.getHQIdx(rc, myHQ);
                return;
            }
        }

        int bestDist = 999999;
        for(int i = 0; i < 4; i++) {
            int encoded = (rc.readSharedArray(i) & 0b0111_1111_1111_1111) - 1;
            if(encoded == -1) continue;
            MapLocation hq = new MapLocation(encoded/60,encoded%60);
            if(hq.distanceSquaredTo(rc.getLocation()) < bestDist && !getSurrounded(rc, i)) {
                myHQ = hq;
                bestDist = hq.distanceSquaredTo(rc.getLocation());
            }
        }
    }

    public static void recordWells(RobotController rc) {
        WellInfo[] wells = rc.senseNearbyWells();
        for(WellInfo well : wells) {
            MapLocation m = well.getMapLocation();
            wellsSeen.putIfAbsent(m, well.getResourceType());
        }
    }

    static void collectResources(RobotController rc) throws GameActionException {
        if(myResource == null || myWell == null) return;
        if(rc.getResourceAmount(myResource) == 39) return;
        if(!rc.canSenseLocation(myWell)) return;
        WellInfo well = rc.senseWell(myWell);

        if(rc.canCollectResource(well.getMapLocation(), well.getRate())) {
            rc.collectResource(well.getMapLocation(), well.getRate());
            if(myWell != null && wellFull(rc, myWell)) {
                myWellFull = true;
            }
        }

    }

    static void depositResources(RobotController rc) throws GameActionException {
        if(myResource == null) return;
        if (rc.canWriteSharedArray(0, 0)) {
            for (Map.Entry<MapLocation, ResourceType> entry : wellsSeen.entrySet()) {
                MapLocation loc = entry.getKey();
                ResourceType resource = entry.getValue();
                Communicator.storeWellInfo(rc, loc.x, loc.y, resource, myWell != null && loc.equals(myWell) && myWellFull);
            }
        }

        RobotInfo[] robots = rc.senseNearbyRobots(1000, rc.getTeam());
        for(RobotInfo robot : robots) {
            if(robot.getType() == RobotType.HEADQUARTERS) {
                if(rc.canTransferResource(robot.getLocation(), myResource, rc.getResourceAmount(myResource))) {
                    rc.transferResource(robot.getLocation(), myResource, rc.getResourceAmount(myResource));
                }
                goingBackToHQ = false;
                goingBackTurns = 0;
            }
        }
    }

    static boolean anchorStuff(RobotController rc) throws GameActionException {
        if(State.getState(rc) == State.COMPLETE_CONTROL) {
            if(rc.getAnchor() != null) {
                int islan = rc.senseIsland(rc.getLocation());
                if(islan != -1 && rc.senseAnchor(islan) == null) {
                    rc.placeAnchor();
                    return true;
                }
                int[] islands = rc.senseNearbyIslands();
                for(int island : islands) {
                    if(island == -1)continue;
                    if(rc.senseAnchor(island) != null) continue;
                    MapLocation[] locs = rc.senseNearbyIslandLocations(island);
                    MapLocation myLoc = rc.getLocation();

                    MapLocation bestLoc = null;
                    int bestDistance = 999999;
                    for(MapLocation loc : locs) {
                        int distance = myLoc.distanceSquaredTo(loc);
                        if(distance < bestDistance) {
                            bestDistance = distance;
                            bestLoc = loc;
                        }
                    }

                    Pathfinding.navigateToLocationBug(rc, bestLoc);
                    return true;
                }
                return Pathfinding.navigateRandomly(rc);

            }
            RobotInfo[] allies = rc.senseNearbyRobots(1000, rc.getTeam());
            for(RobotInfo ally : allies) {
                if(ally.getType() == RobotType.HEADQUARTERS) {
                    if(rc.canTakeAnchor(ally.getLocation(), Anchor.STANDARD)) {
                        rc.takeAnchor(ally.getLocation(), Anchor.STANDARD);
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
