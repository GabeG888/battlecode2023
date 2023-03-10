package v6;

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
    static int turnsAlive = 0;
    public static void run(RobotController rc) throws GameActionException {
        if(myHQ == null) initHQ(rc);
        if(myWell == null && !scout && rc.canWriteSharedArray(0, 0)) receiveAssignment(rc);

        if(turnsAlive < 3 && myHQ != null) {
            Pathfinding.navigateAwayFrom(rc, myHQ);
            turnsAlive++;
        }
        depositResources(rc);
        if(anchorStuff(rc)) return;

        collectResources(rc);

        recordWells(rc);

        boolean moved = false;
        RobotInfo[] enemies = rc.senseNearbyRobots(1000, rc.getTeam().opponent());
        for(RobotInfo enemy : enemies) {
            if(enemy.getType() == RobotType.LAUNCHER && rc.canAttack(enemy.getLocation())) {
                rc.attack(enemy.getLocation());
            }
            if(enemy.getType() == RobotType.LAUNCHER && myHQ != null) {
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

        if (myResource != null && rc.getResourceAmount(myResource) > 38) {
            Pathfinding.navigateToLocationBug(rc, myHQ);
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
        }
        if(myWell == null) {
            moved = Pathfinding.navigateRandomly(rc);
        }

        recordWells(rc);

        //if(moved)
        //    MapStore.updateMap(rc);
        if(myWell != null)  rc.setIndicatorString(myWell.toString() + " " + myResource.toString() + "; FULL:  " + myWellFull);
    }

    static boolean wellFull(RobotController rc, MapLocation well) throws GameActionException {
        boolean full = true;
        for(Direction d : directions) {
            MapLocation spot = well.add(d);
            if(!rc.canSenseLocation(spot)) full = false;
            if(rc.canSenseLocation(spot) && rc.sensePassability(spot)) {
                RobotInfo robotAtSpot = rc.senseRobotAtLocation(spot);
                if(robotAtSpot == null || robotAtSpot.getType() != RobotType.CARRIER || robotAtSpot.getTeam() == rc.getTeam())
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
                    if(!rc.canSenseLocation(spot)) full = false;
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
                    return Pathfinding.navigateToLocationBug(rc, bestSpot);
                }
            }
            return Pathfinding.navigateToLocationBug(rc, myWell);
        }
        return false;
    }

    static void receiveAssignment(RobotController rc) throws GameActionException {
        for(int i = 4; i <= 23; i++) {
            int wellState = rc.readSharedArray(i);
            Assignment a = new Assignment(wellState);
            if(a.num > 0) {
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
    }

    public static void recordWells(RobotController rc) {
        WellInfo[] wells = rc.senseNearbyWells();
        for(WellInfo well : wells) {
            wellsSeen.putIfAbsent(well.getMapLocation(), well.getResourceType());
        }
    }

    static void collectResources(RobotController rc) throws GameActionException {
        if(myResource == null) return;
        if(rc.getResourceAmount(myResource) == 39) return;
        WellInfo[] wells = rc.senseNearbyWells(myResource);
        if(myWell != null && wellFull(rc, myWell)) {
            myWellFull = true;
        }
        for(WellInfo well : wells) {
            if(rc.canCollectResource(well.getMapLocation(), well.getRate())) {
                rc.collectResource(well.getMapLocation(), well.getRate());
            }
        }
    }

    static void depositResources(RobotController rc) throws GameActionException {
        if(myResource == null) return;

        for(Map.Entry<MapLocation, ResourceType> entry : wellsSeen.entrySet()) {
            MapLocation loc = entry.getKey();
            ResourceType resource = entry.getValue();
            if (rc.canWriteSharedArray(0, 0)) {
                Communicator.storeWellInfo(rc, loc.x, loc.y, resource, myWell != null && loc.equals(myWell) && myWellFull);
            }
        }

        RobotInfo[] robots = rc.senseNearbyRobots(1000, rc.getTeam());
        for(RobotInfo robot : robots) {
            if(robot.getType() == RobotType.HEADQUARTERS &&
                    rc.canTransferResource(robot.getLocation(), myResource, rc.getResourceAmount(myResource))) {
                rc.transferResource(robot.getLocation(), myResource, rc.getResourceAmount(myResource));
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
