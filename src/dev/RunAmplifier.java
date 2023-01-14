package dev;

import battlecode.common.*;

import static dev.RobotPlayer.*;

public strictfp class RunAmplifier {
    /**
     * Run a single turn for a Amplifier.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
    static String destName = "DEST_AMPLIFIER";
    static void runAmplifier(RobotController rc) throws GameActionException {

        // look for directions in shared array
        int index = findIndex(rc, destName);
        if (index != -1) {
            rc.setIndicatorString("moving towards my destination");
            navigateTo(rc, decrypt(rc, index));
        }

        // Move randomly
        Direction dir = directions[currentDirectionInd];
        if (rc.canMove(dir)) {
            rc.move(dir);
        } else if (rc.getMovementCooldownTurns() == 0) {
            currentDirectionInd = rng.nextInt(directions.length);
        }
    }
}