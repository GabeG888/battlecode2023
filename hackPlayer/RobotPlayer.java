package hackPlayer;

import battlecode.common.*;

import java.lang.instrument.*;


@SuppressWarnings("unused")
public strictfp class RobotPlayer {
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

    }

    static void runCarrier(RobotController rc) throws GameActionException {

    }

    static void runLauncher(RobotController rc) throws GameActionException {

    }
}
