package v4shell;

import battlecode.common.*;
import java.util.*;

public class Carrier {

    static MapLocation myHQ = null;
    static MapLocation myWell = null;
    static ResourceType myResource;
    static boolean scout = false;

    public static void run(RobotController rc) throws GameActionException {
        if(myHQ == null) initHQ(rc);
        if(myWell == null && !scout && rc.canWriteSharedArray(0, 0)) receiveAssignment(rc);

        collectResources(rc);
        depositResources(rc);

        boolean moved = false;
        RobotInfo[] enemies = rc.senseNearbyRobots(1000, rc.getTeam().opponent());
        for(RobotInfo enemy : enemies) {
            if(enemy.getType() != RobotType.HEADQUARTERS && rc.canAttack(enemy.getLocation())) {
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

        if (myResource != null && rc.getResourceAmount(myResource) > 36) {
            Pathfinding.navigateToLocationBug(rc, myHQ);
        }
        else if(myWell != null) {
            moved = Pathfinding.navigateToLocationBug(rc, myWell);
        }
        else if(scout) {
            WellInfo[] manaWells = rc.senseNearbyWells(ResourceType.MANA);
            if(manaWells.length > 0) {
                for(WellInfo manaWell : manaWells) {
                    if(Communicator.alreadyRecorded(rc, manaWell.getMapLocation())) continue;
                    myResource = ResourceType.MANA;
                    myWell = manaWell.getMapLocation();
                    scout = false;
                }
            }

            if(myResource == null) {
                WellInfo[] adamWells = rc.senseNearbyWells(ResourceType.ADAMANTIUM);
                if(adamWells.length > 0) {
                    for(WellInfo adamWell : adamWells) {
                        if(Communicator.alreadyRecorded(rc, adamWell.getMapLocation())) continue;
                        myResource = ResourceType.ADAMANTIUM;
                        myWell = adamWell.getMapLocation();
                        scout = false;
                    }
                }
            }

            if(myWell != null) {
                moved = Pathfinding.navigateToLocationBug(rc, myWell);
            }
            else moved = Pathfinding.navigateRandomly(rc);
        }

        if(moved)
            MapStore.updateMap(rc);
    }

    static void receiveAssignment(RobotController rc) throws GameActionException {
        for(int i = 4; i <= 62; i++) {
            int wellState = rc.readSharedArray(i);
            int num = wellState >> 13;
            if(num > 0) {
                num--;
                rc.writeSharedArray(i, (wellState & 0b1_1111_1111_1111) | (num << 13));
                int encoded = (wellState & 0b0_1111_1111_1111) - 1;
                myWell = new MapLocation(encoded/60, encoded%60);
                myResource = (wellState & 0b1_0000_0000_0000) > 0 ? ResourceType.ADAMANTIUM : ResourceType.MANA;
                return;
            }
        }
        scout = true;
    }

    static void initHQ(RobotController rc) throws GameActionException {
        for(RobotInfo robot : rc.senseNearbyRobots(1000, rc.getTeam())) {
            if(robot.getType() == RobotType.HEADQUARTERS) {
                myHQ = robot.getLocation();
                return;
            }
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
        RobotInfo[] robots = rc.senseNearbyRobots(1000, rc.getTeam());
        for(RobotInfo robot : robots) {
            if(robot.getType() == RobotType.HEADQUARTERS &&
                    rc.canTransferResource(robot.getLocation(), myResource, rc.getResourceAmount(myResource))) {
                rc.transferResource(robot.getLocation(), myResource, rc.getResourceAmount(myResource));
            }
        }
    }
}
