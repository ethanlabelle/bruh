package dev;

import battlecode.common.*;

import static dev.RobotPlayer.*;

import java.util.Arrays;

public strictfp class RunAmplifier {
    /**
     * Run a single turn for a Amplifier.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
    static String destName = "DEST_AMPLIFIER";
    static String destLauncher = "DEST_LAUNCHER";
    static boolean isAttack = false;
    static int attackIndex;
    static void runAmplifier(RobotController rc) throws GameActionException {
        //System.out.println("the attack index is " + attackIndex + " are we attacking " + isAttack);
        // run away from enemies and sets target for launchers
        Team opponent = RobotPlayer.myTeam.opponent();
        RobotInfo[] enemies = Arrays.stream(rc.senseNearbyRobots(-1, opponent)).filter(robot -> robot.type != RobotType.HEADQUARTERS).toArray(RobotInfo[]::new);
        if (enemies.length > 0) {
            if (isAttack) {
                attackIndex = communicate (rc, destLauncher, enemies[0].getLocation(), attackIndex);
            } else {
                attackIndex = communicate (rc, destLauncher, enemies[0].getLocation());
                //System.out.println("the new attack index is " + attackIndex);
                isAttack = true;
            }
            rc.setIndicatorString("attacking at specific location");
            Direction away = oppositeDirection(rc.getLocation().directionTo(enemies[0].getLocation()));
            if (rc.canMove(away)) {
                rc.move(away);
            }
        } else {
            //deleteCommunication (rc, attackIndex);
            //isAttack = false;
        }

        // look for directions in shared array
        int index = findIndex(rc, destName);
        if (index != -1) {
            rc.setIndicatorString("moving towards my destination");
            navigateTo(rc, decrypt(rc, index));
        }
        
        // Move randomly
        Direction dir = directions[currentDirectionInd];
        if (rc.canMove(dir)) {
            rc.setIndicatorString("moving randomly");
            rc.move(dir);
        } else if (rc.getMovementCooldownTurns() == 0) {
            currentDirectionInd = rng.nextInt(directions.length);
        }
    }
}