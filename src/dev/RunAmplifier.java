package dev;

import battlecode.common.*;

import static dev.RobotPlayer.*;

public strictfp class RunAmplifier {
    /**
     * Run a single turn for a Amplifier.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
    static void runAmplifier(RobotController rc) throws GameActionException {
		updateMap(rc);
		Communication.tryWriteMessages(rc);
        // Move randomly
        Direction dir = directions[currentDirectionInd];
        if (rc.canMove(dir)) {
            rc.move(dir);
        } else if (rc.getMovementCooldownTurns() == 0) {
            currentDirectionInd = rng.nextInt(directions.length);
        }
    }
}
