package dev;


import battlecode.common.*;
import java.util.Arrays;

import static dev.RobotPlayer.*;

public strictfp class RunHeadquarters {

	static final int LAUNCHER_MOD = 10;
	static final int CARRIER_MOD = 5;
	static final int MAX_CARRIERS = 15;
	static final int EXCESS = 160;
	static int launcherCount = 0;
	static int carrierCount = 0;

	// number of enemy robots for lockdown
	static final int MAX_ENEMIES = 4;
	static final int SPAWN_AMOUNT = 5;

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
		RobotInfo[] enemies = Arrays.stream(robotInfos).filter(robot -> robot.team != myTeam).toArray(RobotInfo[]::new);	

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
		if (enemies.length > 0 && rc.getResourceAmount(ResourceType.MANA) > enemies.length * RobotType.LAUNCHER.buildCostMana * 2) {
			MapLocation loc = null;
			while (rc.getResourceAmount(ResourceType.MANA) > EXCESS) {
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
				Clock.yield();
				turnCount++;
			}
			return;
		}

        // Pick a direction to build in.
        if (rc.canBuildAnchor(Anchor.STANDARD) && rc.getNumAnchors(Anchor.STANDARD) == 0) {
            // If we can build an anchor do it!
            rc.buildAnchor(Anchor.STANDARD);
            rc.setIndicatorString("Building anchor!");
			launcherCount = 0;
			carrierCount = 0;
        }

		MapLocation loc;
        //if ((launcherCount + 1) % LAUNCHERS_PER_AMPLIFIER == 0) {
        if (turnCount % 75 == 0 || (!hasSpawnedAmplifier && rc.getResourceAmount(ResourceType.MANA) > EXCESS)) {
            loc = getSpawnLocation(rc, RobotType.AMPLIFIER);
            if (loc != null) {
                rc.buildRobot(RobotType.AMPLIFIER, loc);
				hasSpawnedAmplifier = true;
                //launcherCount++;
                return;
            }
        }
        // Let's try to build a launcher.
		// if (launcherCount < LAUNCHER_MOD || rc.getResourceAmount(ResourceType.MANA) > EXCESS) {
            // rc.setIndicatorString("Trying to build launchers");
            //launcherCount += buildNLaunchers(rc, 2);
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
		// }

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
	// max out elixir for destabilizers then go to launchers, perhaps better implementation later
	static RobotType [] getBuild (RobotController rc) {
		int numDestabilizers = rc.getResourceAmount(ResourceType.ELIXIR) / RobotType.DESTABILIZER.buildCostElixir;
		if (numDestabilizers >= SPAWN_AMOUNT) {
			RobotType [] holder = new RobotType [SPAWN_AMOUNT];
			for (int index = 0; index < SPAWN_AMOUNT; index ++) {
				holder[index] = RobotType.DESTABILIZER;
			}
			return holder;
		}
		int numLaunchers = rc.getResourceAmount(ResourceType.MANA) / RobotType.LAUNCHER.buildCostMana;
		if (numDestabilizers + numLaunchers >= SPAWN_AMOUNT) {
			RobotType [] holder = new RobotType [SPAWN_AMOUNT];
			for (int index = 0; index < numDestabilizers; index ++) {
				holder[index] = RobotType.DESTABILIZER;
			}
			for (int index = numDestabilizers; index < SPAWN_AMOUNT; index ++) {
				holder[index] = RobotType.LAUNCHER;
			}
			return holder;
		}
		return null;
	}

     static int buildNLaunchers(RobotController rc, int n) throws GameActionException {
        int i = 0;
        while (rc.isActionReady() && i < n) {
            MapLocation spawnLoc = getSpawnLocation(rc, RobotType.LAUNCHER);
            if (spawnLoc != null) {
                           rc.buildRobot(RobotType.LAUNCHER, spawnLoc);
                i++;
             }
        }
        return i;
    }

	static void setup(RobotController rc) throws GameActionException {
		RobotInfo[] robotInfos = rc.senseNearbyRobots(-1, enemyTeam);
		for (int i = robotInfos.length; --i >= 0; ) {
			if (robotInfos[i].type == RobotType.HEADQUARTERS) {
				Communication.reportEnemyHeadquarters(rc, robotInfos[i].location);
			}
		}
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
}
