package v4shell;

import battlecode.common.*;

import java.util.Arrays;
import java.util.Comparator;

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
    static boolean attackNearby(RobotController rc) throws GameActionException{
        RobotInfo[] enemies = rc.senseNearbyRobots(1000, rc.getTeam().opponent());
        MapLocation myLoc = rc.getLocation();

        Arrays.sort(enemies, Comparator.comparingInt(Launcher::getAttackPriority).thenComparingInt(RobotInfo::getHealth));

        for(RobotInfo enemy : enemies) {
            if(rc.canAttack(enemy.getLocation())) {
                rc.attack(enemy.getLocation());
                return true;
            }
        }
        return false;
    }
    static void run(RobotController rc) throws GameActionException {
        boolean attacked = false;
        boolean moved = false;

        attackNearby(rc);
    }

}
