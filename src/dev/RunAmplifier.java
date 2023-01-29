package dev;

import battlecode.common.*;

import static dev.RobotPlayer.*;

public strictfp class RunAmplifier {
    static MapLocation me;
    static RobotInfo[] enemyRobots;
    static RobotInfo[] friendlyRobots;

    /**
     * Run a single turn for a Amplifier.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
    static void runAmplifier(RobotController rc) throws GameActionException {
        updateMap(rc);

        me = rc.getLocation();
        enemyRobots = rc.senseNearbyRobots(-1, enemyTeam);
        friendlyRobots = rc.senseNearbyRobots(-1, myTeam);

        // run away from enemy launchers
		if (enemyRobots.length > 0) {
            for (RobotInfo robot: enemyRobots) {
                if (robot.type == RobotType.LAUNCHER) {
			        Communication.reportEnemy(rc, robot.location);
                    runAway(rc);
                }
            }
		}

        // run away from friendly amplifiers
        if (friendlyRobots.length > 0) {
            me = rc.getLocation();
            MapLocation dest = me;
            int i = friendlyRobots.length;
            while (--i >= 0) {
                if (friendlyRobots[i].type == RobotType.AMPLIFIER) {
                    dest = dest.add(me.directionTo(friendlyRobots[i].location).opposite());
                }
            }
            if (!dest.equals(me)) {
                tryMove(rc, me.directionTo(dest));
            }
        }

		// look for targets to defend
		MapLocation defLoc = Communication.getClosestEnemy(rc);
		if (defLoc != null) {
			navigateTo(rc, defLoc);
            Communication.clearObsoleteEnemies(rc);
            return;
		}

        if (defLoc == null) {
            MapLocation neutralIslandLoc = null;
            for (int i = 1; i <= GameConstants.MAX_NUMBER_ISLANDS; i++) {
                if (Communication.readTeamHoldingIsland(rc, i) == Team.NEUTRAL && Communication.readIslandLocation(rc, i) != null) {
                    neutralIslandLoc = Communication.readIslandLocation(rc, i);
                    break;
                }
            }
            if (neutralIslandLoc != null)
                navigateTo(rc, neutralIslandLoc);
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
