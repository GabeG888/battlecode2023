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
    static void attackNearby(RobotController rc) throws GameActionException{
        while(rc.isActionReady()) {
            boolean attacked = false;
            RobotInfo[] enemies = rc.senseNearbyRobots(1000, rc.getTeam().opponent());

            Arrays.sort(enemies, Comparator.comparingInt(Launcher::getAttackPriority).thenComparingInt(RobotInfo::getHealth));

            for(RobotInfo enemy : enemies) {
                if(rc.canAttack(enemy.getLocation())) {
                    rc.attack(enemy.getLocation());
                    attacked = true;
                    break;
                }
            }
            if(!attacked) break;
        }
    }

    static void run(RobotController rc) throws GameActionException {
        RobotInfo[] enemies = rc.senseNearbyRobots(1000, rc.getTeam().opponent());
        RobotInfo[] allies = rc.senseNearbyRobots(1000, rc.getTeam());

        int enemyLauchers = 0;
        for(RobotInfo enemy : enemies) {
            if(enemy.getType() == RobotType.LAUNCHER) enemyLauchers++;
        }
        int allyLaunchers = 0;
        for(RobotInfo ally : allies) {
            if(ally.getType() == RobotType.LAUNCHER) allyLaunchers++;
        }

        if(enemies.length > 0) {
            attackNearby(rc);
            if(enemyLauchers > allyLaunchers) {
                Pathfinding.navigateAwayFrom(rc, enemies[0].getLocation());
            }
            else if(enemyLauchers < allyLaunchers) {
                Pathfinding.navigateToLocationFuzzy(rc, enemies[0].getLocation());
            }
        }
        Pathfinding.navigateRandomly(rc);
    }
}
