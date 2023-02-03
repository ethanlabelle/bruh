package dev;


import battlecode.common.*;
import java.util.Arrays;

import static dev.RobotPlayer.*;

public strictfp class RunHeadquarters {

	static final int LAUNCHER_MOD = 20;
	static final int LAUNCHERS_PER_AMPLIFIER = 10;
	static final int CARRIER_MOD = 8;
	static final int MAX_CARRIERS = 10;
	static final int EXCESS = 100;
	static int launcherCount = 0;
	static int carrierCount = 0;
	static WellInfo[] nearbyWells;
	// number of enemy robots for lockdown
	static final int MIN_ENEMIES = 5;

	static boolean hasSpawnedAmplifier = false;

    /**
     * Run a single turn for a Headquarters.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     *
     */
    static void runHeadquarters(RobotController rc) throws GameActionException {
		// scan for enemies- calls for help if needed
		RobotInfo[] robotInfos = rc.senseNearbyRobots();
		RobotInfo[] carriers = Arrays.stream(robotInfos).filter(robot -> robot.type == RobotType.CARRIER && robot.team == myTeam).toArray(RobotInfo[]::new);	
		RobotInfo[] enemies = Arrays.stream(robotInfos).filter(robot -> robot.team != myTeam && robot.type == RobotType.LAUNCHER).toArray(RobotInfo[]::new);	

		if (enemies.length > 0) {
            for (RobotInfo robot: enemies) {
                if (robot.type == RobotType.LAUNCHER) {
			        Communication.reportEnemy(rc, robot.location);
                }
            }
		}

		// write to shared array outstanding messages
		Communication.tryWriteMessages(rc);
		Communication.clearObsoleteEnemies(rc);

		// if max enemies are reached wait to build robots
		if (enemies.length >= MIN_ENEMIES) {
			if (rc.getResourceAmount(ResourceType.MANA) > enemies.length * RobotType.LAUNCHER.buildCostMana * 2) {
				MapLocation loc = null;
				while (rc.getResourceAmount(ResourceType.MANA) > EXCESS) {
					while (rc.isActionReady()) {
						loc = getSpawnLocation(rc, RobotType.LAUNCHER);
						if (loc != null) {
							rc.buildRobot(RobotType.LAUNCHER, loc);
							launcherCount++;
						} else {
							break;
						}
					}
					Clock.yield();
					turnCount++;
				}
			} else return;
		}

		MapLocation loc;
        if (turnCount % 100 == 0 || (!hasSpawnedAmplifier && rc.getResourceAmount(ResourceType.MANA) > EXCESS)) {
            loc = getSpawnLocation(rc, RobotType.AMPLIFIER);
            if (loc != null) {
                rc.buildRobot(RobotType.AMPLIFIER, loc);
				hasSpawnedAmplifier = true;
                return;
            }
        }

        // Pick a direction to build in.
        if (rc.canBuildAnchor(Anchor.STANDARD) && rc.getNumAnchors(Anchor.STANDARD) == 0) {
            // If we can build an anchor do it!
            rc.buildAnchor(Anchor.STANDARD);
            rc.setIndicatorString("Building anchor!");
			launcherCount = 0;
			carrierCount = 0;
        }
		
        // Let's try to build a launcher.
		if (launcherCount < LAUNCHER_MOD || rc.getResourceAmount(ResourceType.MANA) > EXCESS) {
			while (rc.isActionReady()) {
				rc.setIndicatorString("Trying to build launchers");
				loc = getSpawnLocation(rc, RobotType.LAUNCHER);
				if (loc != null) {
					rc.buildRobot(RobotType.LAUNCHER, loc);
					launcherCount++;
				} else {
					break;
				}
			}
		}

        // Let's try to build a carrier.
		if ((carriers.length <= MAX_CARRIERS) && (carrierCount < CARRIER_MOD || rc.getResourceAmount(ResourceType.ADAMANTIUM) > EXCESS)) {
        	while (rc.isActionReady()) {
				rc.setIndicatorString("Trying to build a carrier");
				loc = getSpawnLocation(rc, RobotType.CARRIER);
				if (loc != null) {
					rc.buildRobot(RobotType.CARRIER, loc);
					carrierCount++;
				} else {
					break;
				}
			}
		}
    }

	static void setup(RobotController rc) throws GameActionException {
		Communication.initSymmetry(rc);
		RobotInfo[] robotInfos = rc.senseNearbyRobots(-1, enemyTeam);
		nearbyWells = rc.senseNearbyWells(); // 100 bytecode
		for (int i = robotInfos.length; --i >= 0; ) {
			if (robotInfos[i].type == RobotType.HEADQUARTERS) {
				Communication.reportEnemyHeadquarters(rc, robotInfos[i].location);
			}
		}
		for (int i = nearbyWells.length; --i >= 0;) {
			WellInfo wellInfo = nearbyWells[i];
			MapLocation arrayLoc;
			MapLocation loc = wellInfo.getMapLocation();
			switch (wellInfo.getResourceType()) {
				case MANA:
					arrayLoc = Communication.readManaWellLocation(rc, HQLOC);
					if (arrayLoc == null || loc.distanceSquaredTo(HQLOC) < arrayLoc.distanceSquaredTo(HQLOC))
						Communication.updateManaWellLocation(rc, loc, HQLOC);
					Communication.addManaWell(rc, loc);
					break;
				case ADAMANTIUM:
					arrayLoc = Communication.readAdaWellLocation(rc, HQLOC);
					if (arrayLoc == null || loc.distanceSquaredTo(HQLOC) < arrayLoc.distanceSquaredTo(HQLOC))
						Communication.updateAdaWellLocation(rc, loc, HQLOC);
					break;
				case ELIXIR:
					break;
				default:
					break;
			}
		}
		Communication.tryWriteMessages(rc);
		int i = 0;
		while (i < 4) {
			rc.setIndicatorString("Trying to build a launcher");
			MapLocation loc = getSpawnLocation(rc, RobotType.LAUNCHER);
			if (loc != null) {
				rc.buildRobot(RobotType.LAUNCHER, loc);
				i++;
			} else {
				Clock.yield();
				turnCount++;
			}
		}	
		i = 0;
		while (i < 4) {
			rc.setIndicatorString("Trying to build a carrier");
			MapLocation loc = getSpawnLocation(rc, RobotType.CARRIER);
			if (loc != null) {
				rc.buildRobot(RobotType.CARRIER, loc);
				i++;
			} else {
				Clock.yield();
				turnCount++;
			}
		}
	}

	static MapLocation getClosestLocation (RobotController rc, MapLocation loc, RobotType unit) throws GameActionException {
		// this is the possible locations it can be the closest to
		MapLocation[] possLoc = rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), RobotType.HEADQUARTERS.actionRadiusSquared);
		int minDist = 7200;
		MapLocation bestLoc = null;
		for (MapLocation checkLoc : possLoc) {
			// rc.canSenseRobotAtLocation(MapLocation loc) always returned false, spawned robot on top of robot and deleted headquarters
			if (!checkLoc.equals(rc.getLocation()) && rc.canBuildRobot(unit, checkLoc)) {
				int checkDist = checkLoc.distanceSquaredTo(loc);
				if (checkDist < minDist) {
					bestLoc = checkLoc;
					minDist = checkDist;
				}
			}
		}
		return bestLoc;
	}

	static MapLocation getSpawnLocation(RobotController rc, RobotType unit) throws GameActionException {
		if (unit == RobotType.CARRIER) {
			switch (rc.getRoundNum()) {
				case 1:
					MapLocation adaWell = Communication.getClosestWell(rc, ResourceType.ADAMANTIUM);
					if (adaWell != null) {
						MapLocation closeTile = getClosestLocation(rc, adaWell, unit);
						if (closeTile != null) {
							return closeTile;
						}
					}
					break;
				default:
					MapLocation manaWell = Communication.getClosestWell(rc, ResourceType.MANA);
					if (manaWell != null) {
						MapLocation closeTile = getClosestLocation(rc, manaWell, unit);
						if (closeTile != null) {
							return closeTile;
						}
					}
					break;
			}
			if (nearbyWells.length > 0) {
				MapLocation closeWell = getClosestLocation(rc, nearbyWells[0].getMapLocation(), unit);
				if (closeWell != null) {
					return closeWell;
				}
			}
		}

		MapLocation center = new MapLocation(width/2, height/2);
		MapLocation spawnLoc = getClosestLocation(rc, center, unit);
		return spawnLoc;
	}
}
