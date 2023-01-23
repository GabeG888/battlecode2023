package v5o1;

import battlecode.common.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class MapStore {

    static Map<MapLocation, CustomMapInfo> map = new HashMap<>();

    public static void updateMap(RobotController rc) throws GameActionException {
        for(MapInfo mi : rc.senseNearbyMapInfos()) {
            MapLocation loc = mi.getMapLocation();

            if(map.containsKey(loc)) continue;
            if(!rc.canSenseLocation(loc)) continue;

            CustomMapInfo cmi =
                    new CustomMapInfo(mi.getCurrentDirection(), mi.isPassable(),
                                        mi.hasCloud(), rc.senseIsland(loc) != -1, null);
            if(rc.senseWell(loc) != null) {
                cmi.well = rc.senseWell(loc).getResourceType();
            }

            map.put(loc, cmi);
        }
    }
    public static final int UPDOWN = 1;
    public static final int LEFTRIGHT = 2;
    public static final int ROTATIONAL = 4;
    public static int possibleSymmetry = 7;
    static HashSet<MapLocation> seen = new HashSet<>();

    // Returns true is symmetry was found.
    public static boolean computeSymmetry(RobotController rc) {
        int w = rc.getMapWidth()-1, h = rc.getMapHeight()-1;
        for(Map.Entry<MapLocation, CustomMapInfo> entry : map.entrySet()) {
            MapLocation key = entry.getKey();
            if(seen.contains(key)) continue;
            CustomMapInfo value = entry.getValue();
            if((possibleSymmetry & UPDOWN) == UPDOWN) {
                MapLocation sym = new MapLocation(w - key.x, key.y);
                if(map.containsKey(sym) && !map.get(sym).equals(value)) {
                    possibleSymmetry &= ~UPDOWN;
                }
            }
            if((possibleSymmetry & LEFTRIGHT) == LEFTRIGHT) {
                MapLocation sym = new MapLocation(key.x, h - key.y);
                if(map.containsKey(sym) && !map.get(sym).equals(value)) {
                    possibleSymmetry &= ~LEFTRIGHT;
                }
            }
            if((possibleSymmetry & ROTATIONAL) == ROTATIONAL) {
                MapLocation sym = new MapLocation(w - key.x, h - key.y);
                if(map.containsKey(sym) && !map.get(sym).equals(value)) {
                    possibleSymmetry &= ~ROTATIONAL;
                }
            }
            seen.add(key);
        }
        return (possibleSymmetry == ROTATIONAL || possibleSymmetry == UPDOWN || possibleSymmetry == LEFTRIGHT);
    }

    public static void computeInitialSymmetry(RobotController rc) throws GameActionException {
        int i = 0;
        int hy = rc.getMapHeight()/2;
        int hx = rc.getMapWidth()/2;
        boolean up = true, down = true, left = true, right = true;
        while(i < 4) {
            int hqLoc = rc.readSharedArray(i);
            if(hqLoc == 0) {
                break;
            }
            i++;
            int x = hqLoc / 60, y = hqLoc % 60;
            if(x < hx) left = false;
            else right = false;
            if(y < hy) down = false;
            else up = false;
        }

        if(!right && !left) possibleSymmetry &= ~LEFTRIGHT;
        else if(!up && !down) possibleSymmetry &= ~UPDOWN;
    }
}
class CustomMapInfo {

    int states = 0;
    public CustomMapInfo(Direction curDir, boolean passable, boolean cloud, boolean island, ResourceType well) {
        this.curDir = curDir;
        this.states = (passable ? 1 : 0) | (cloud ? 1 : 0) << 1 | (island ? 1 : 0) << 2;
        this.well = well;
    }

    ResourceType well;
    Direction curDir;

    public boolean equals(CustomMapInfo cmi) {
        return cmi.curDir == this.curDir && this.states == cmi.states && this.well == cmi.well;
    }
}