package v15;


import battlecode.common.*;
import java.util.Arrays;

import static v15.RobotPlayer.*;

public strictfp class RunHeadquarters {

	static final int LAUNCHER_MOD = 10;
	static final int LAUNCHERS_PER_AMPLIFIER = 10;
	static final int CARRIER_MOD = 5;
	static final int MAX_CARRIERS = 25;
	static final int EXCESS = 160;
	static int launcherCount = 0;
	static int carrierCount = 0;

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
			Communication.reportEnemy(rc, rc.getLocation());
		}
		// write to shared array outstanding messages
		Communication.tryWriteMessages(rc);
		Communication.clearObsoleteEnemies(rc);


        // Pick a direction to build in.
        if (rc.canBuildAnchor(Anchor.STANDARD) && rc.getNumAnchors(Anchor.STANDARD) == 0) {
            // If we can build an anchor do it!
            rc.buildAnchor(Anchor.STANDARD);
            rc.setIndicatorString("Building anchor!");
			launcherCount = 0;
			carrierCount = 0;
        }

		MapLocation loc;
        if ((launcherCount + 1) % LAUNCHERS_PER_AMPLIFIER == 0) {
            rc.setIndicatorString("Trying to build a launcher");
            loc = getSpawnLocation(rc, RobotType.AMPLIFIER);
            if (loc != null) {
                rc.buildRobot(RobotType.AMPLIFIER, loc);
                launcherCount++;
                return;
            }
        }
        // Let's try to build a launcher.
		if (launcherCount < LAUNCHER_MOD || rc.getResourceAmount(ResourceType.MANA) > EXCESS || rc.getRoundNum() <= 250) {
        	rc.setIndicatorString("Trying to build a launcher");
        	loc = getSpawnLocation(rc, RobotType.LAUNCHER);
        	if (loc != null) {
        	    rc.buildRobot(RobotType.LAUNCHER, loc);
				launcherCount++;
        	    return;
        	}
		}

        // Let's try to build a carrier.
		if ((carriers.length <= MAX_CARRIERS) && (carrierCount < CARRIER_MOD)) {
        	rc.setIndicatorString("Trying to build a carrier");
        	loc = getSpawnLocation(rc, RobotType.CARRIER);
        	if (loc != null) {
        	    rc.buildRobot(RobotType.CARRIER, loc);
				carrierCount++;
        	    return;
        	}
		}
    }
}
