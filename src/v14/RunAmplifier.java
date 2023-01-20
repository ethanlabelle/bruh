package v14;

import battlecode.common.*;

import static v14.RobotPlayer.*;

public strictfp class RunAmplifier {
    static MapLocation me;
    static RobotInfo[] enemyRobots;
    /**
     * Run a single turn for a Amplifier.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
    static void runAmplifier(RobotController rc) throws GameActionException {
        updateMap(rc);

        me = rc.getLocation();
        enemyRobots = rc.senseNearbyRobots(-1, rc.getTeam().opponent());

        // run away from enemy launchers
		if (enemyRobots.length > 0) {
			Communication.reportEnemy(rc, rc.getLocation());
            runAway(rc);
		}

		// look for targets to defend
		MapLocation defLoc = Communication.getClosestEnemy(rc);
		if (defLoc != null) {
			navigateTo(rc, defLoc);
            Communication.clearObsoleteEnemies(rc);
            return;
		}

        //if (turnCount < 500) {
        //    MapLocation neutralIslandLoc = null;
        //    for (int i = 0; i < GameConstants.MAX_NUMBER_ISLANDS; i++) {
        //        if (Communication.readTeamHoldingIsland(rc, i) == Team.NEUTRAL && Communication.readIslandLocation(rc, i) != null) {
        //            neutralIslandLoc = Communication.readIslandLocation(rc, i);
        //            System.out.println(Communication.readTeamHoldingIsland(rc, i) + " " + neutralIslandLoc);
        //            break;
        //        }
        //    }
        //    if (neutralIslandLoc != null)
        //        navigateTo(rc, neutralIslandLoc);
        //    else {
        //        MapLocation wellLoc = Communication.getClosestWell(rc, ResourceType.MANA);
        //        if (wellLoc != null)
        //            navigateTo(rc, wellLoc);
        //        else
        //            navigateTo(rc, HQLOC);
        //    }
        //    return;
        //}

        // Move randomly
        Direction dir = currentDirection;
        if (rc.canMove(dir)) {
            rc.move(dir);
        } else if (rc.getMovementCooldownTurns() == 0) {
            currentDirection = directions[rng.nextInt(directions.length)];
        }
    }

    static void runAway(RobotController rc) throws GameActionException {
		for (RobotInfo r : enemyRobots) {
			if (r.type == RobotType.LAUNCHER) {
				tryMove(rc, oppositeDirection(me.directionTo(r.location)));
				break;
			}
		}
    }
}
