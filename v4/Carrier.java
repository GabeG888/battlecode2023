package v4;

import battlecode.common.*;
import v3.RobotPlayer;

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
    public static void run(RobotController rc) throws GameActionException {
        if(myHQ == null) initHQ(rc);
        if(myWell == null && !scout && rc.canWriteSharedArray(0, 0)) receiveAssignment(rc);

        if(anchorStuff(rc)) return;

        collectResources(rc);
        depositResources(rc);
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

        if (myResource != null && rc.getResourceAmount(myResource) > 36) {
            Pathfinding.navigateToLocationBug(rc, myHQ);
        }
        else if(myWell != null) {
            moved = navigateToWell(rc);
        }

        if(myResource == null || myResource == ResourceType.MANA) {
            WellInfo[] manaWells = rc.senseNearbyWells(ResourceType.MANA);
            for (WellInfo manaWell : manaWells) {
                //if (Communicator.alreadyRecorded(rc, manaWell.getMapLocation())) continue;
                if(myWell != null && manaWell.getMapLocation().distanceSquaredTo(myHQ) > myWell.distanceSquaredTo(myHQ)) continue;
                myResource = ResourceType.MANA;
                myWell = manaWell.getMapLocation();
                scout = false;
            }
        }
        if(myResource == null) {
            WellInfo[] adamWells = rc.senseNearbyWells(ResourceType.ADAMANTIUM);
            for (WellInfo adamWell : adamWells) {
                if (Communicator.alreadyRecorded(rc, adamWell.getMapLocation())) continue;
                if(myWell != null && adamWell.getMapLocation().distanceSquaredTo(myHQ) > myWell.distanceSquaredTo(myHQ)) continue;

                myResource = ResourceType.ADAMANTIUM;
                myWell = adamWell.getMapLocation();
                scout = false;
            }
        }

        if(myWell != null) {
            moved = navigateToWell(rc);
        }
        else moved = Pathfinding.navigateRandomly(rc);

        recordWells(rc);

        //if(moved)
        //    MapStore.updateMap(rc);
        //if(myWell != null)  rc.setIndicatorString(myWell.toString() + " " + myResource.toString());
    }

    static boolean navigateToWell(RobotController rc) throws GameActionException {
        if(myWell.distanceSquaredTo(rc.getLocation()) > 2) {
            if(rc.canSenseLocation(myWell)) {
                MapLocation bestSpot = null;
                int bestDist = 10000;
                for(Direction d : directions) {
                    MapLocation spot = myWell.add(d);
                    if(rc.canSenseLocation(spot) && rc.sensePassability(spot) && rc.senseRobotAtLocation(spot) == null) {
                        if(spot.distanceSquaredTo(rc.getLocation()) < bestDist) {
                            bestSpot = spot;
                            bestDist = spot.distanceSquaredTo(rc.getLocation());
                        }
                    }
                }
                if(bestSpot != null) {
                    rc.setIndicatorString(bestSpot.toString());
                    return Pathfinding.navigateToLocationFuzzy(rc, bestSpot);
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
        WellInfo[] wells = rc.senseNearbyWells(myResource);
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
            if (rc.canWriteSharedArray(0, 0) && !Communicator.alreadyRecorded(rc, loc)) {
                Communicator.storeWellInfo(rc, loc.x, loc.y, resource);
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
                    Pathfinding.navigateToLocationBug(rc, locs[0]);
                    return true;
                }
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
