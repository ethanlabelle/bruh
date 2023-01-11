package dev;


import battlecode.common.*;

import static dev.RobotPlayer.*;

public strictfp class RunHeadquarters {
    /**
     * Run a single turn for a Headquarters.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     *
     */
    static void runHeadquarters(RobotController rc) throws GameActionException {
        // Pick a direction to build in.
        if (rc.canBuildAnchor(Anchor.STANDARD) && rng.nextInt(10) == 1) {
            // If we can build an anchor do it!
            rc.buildAnchor(Anchor.STANDARD);
            rc.setIndicatorString("Building anchor! " + rc.getAnchor());
        }
        // Let's try to build a launcher.
        rc.setIndicatorString("Trying to build a launcher");
        MapLocation loc = getSpawnLocation(rc, RobotType.LAUNCHER);
        if (loc != null) {
            rc.buildRobot(RobotType.LAUNCHER, loc);
            return;
        }
//        RobotInfo[] carriers = Arrays.stream(rc.senseNearbyRobots()).filter(robot -> robot.type == RobotType.CARRIER && robot.team == myTeam).toArray(RobotInfo[]::new);
        // Let's try to build a carrier.
        rc.setIndicatorString("Trying to build a carrier");
        loc = getSpawnLocation(rc, RobotType.CARRIER);
        if (loc != null && turnCount % 10 == 0) {
            rc.buildRobot(RobotType.CARRIER, loc);
            return;
        }
    }
    /*
        boolean found = false;
        MapLocation newLoc = null;
        for (Direction checkDir : directions) {
            newLoc = rc.getLocation().add(checkDir);
            if (!rc.canSenseRobotAtLocation(newLoc)) {
                found = true;
                break;
            }
        }
        //Direction dir = directions[rng.nextInt(directions.length)];
        //MapLocation newLoc = rc.getLocation().add(dir);
        if (!found) {
            return;
        }

        Can return null if there is not a free tile nearby
        TODO: Check all tiles in action radius
                i.e. sense MapInfos to radius & do set difference with occupied squares
     */
    static MapLocation getSpawnLocation(RobotController rc, RobotType unit) throws GameActionException {
        // Pick a direction to build in.
        for (Direction checkDir : directions) {
            MapLocation newLoc = rc.getLocation().add(checkDir);
            if (rc.canBuildRobot(unit, newLoc))
                return newLoc;
        }
        return null;
    }
}
