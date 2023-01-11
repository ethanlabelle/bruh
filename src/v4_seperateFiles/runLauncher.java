package v4_seperateFiles;

import battlecode.common.*;

import static v4_seperateFiles.RobotPlayer.*;

public strictfp class runLauncher {
    /**
     * Run a single turn for a Launcher.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
    static void runLauncher(RobotController rc) throws GameActionException {
        // Try to attack someone
        int radius = rc.getType().actionRadiusSquared;
        Team opponent = rc.getTeam().opponent();
        RobotInfo[] enemies = rc.senseNearbyRobots(radius, opponent);
        if (enemies.length > 0) {
            MapLocation toAttack = enemies[0].location;
            //MapLocation toAttack = rc.getLocation().add(Direction.EAST);

            if (rc.canAttack(toAttack)) {
                rc.setIndicatorString("Attacking");
                rc.attack(toAttack);
            }
        }

        //TODO: ATTACK ENEMY HEADQUARTERS!!!!!!!!!!!!!!!!

        // Also try to move *randomly*.
        Direction dir = directions[currentDirectionInd];
        if (rc.canMove(dir)) {
            rc.move(dir);
        } else if (rc.getMovementCooldownTurns() == 0) {
            currentDirectionInd = rng.nextInt(directions.length);
        }
    }
}