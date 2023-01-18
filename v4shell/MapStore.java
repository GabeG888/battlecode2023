package v4shell;

import battlecode.common.*;

import java.util.HashMap;
import java.util.Map;

public class MapStore {

    static Map<MapLocation, CustomMapInfo> map = new HashMap<>();

    public static void update(RobotController rc) throws GameActionException {
        for(MapInfo mi : rc.senseNearbyMapInfos()) {
            MapLocation loc = mi.getMapLocation();
            CustomMapInfo cmi = new CustomMapInfo(mi.getCurrentDirection(), mi.isPassable(), mi.hasCloud());
            map.put(loc, cmi);
        }
    }
}
class CustomMapInfo {

    public Direction get_curDir() {
        return curDir;
    }

    public boolean is_passable() {
        return passable;
    }

    public boolean is_cloud() {
        return cloud;
    }

    public CustomMapInfo(Direction curDir, boolean passable, boolean cloud) {
        this.curDir = curDir;
        this.passable = passable;
        this.cloud = cloud;
    }

    Direction curDir;
    boolean passable;
    boolean cloud;

}