package dev;

import battlecode.common.*;

import static dev.RobotPlayer.*;

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

        if (turnCount < 500) {
            MapLocation wellLoc = Communication.getClosestWell(rc, ResourceType.MANA);
            if (wellLoc != null)
                navigateTo(rc, wellLoc);
            else
                navigateTo(rc, HQLOC);
        }

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
