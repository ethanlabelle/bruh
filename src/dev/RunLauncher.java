package dev;

import battlecode.common.*;
import java.util.Arrays;

import static dev.RobotPlayer.*;

public strictfp class RunLauncher {
    /**
     * Run a single turn for a Launcher.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
    static boolean at_hq = false;

    static void runLauncher(RobotController rc) throws GameActionException {
        // Try to attack someone
        int radius = rc.getType().actionRadiusSquared;
        Team opponent = rc.getTeam().opponent();
        MapLocation me = rc.getLocation();
        RobotInfo[] enemies = Arrays.stream(rc.senseNearbyRobots(radius, opponent)).filter(robot -> robot.type != RobotType.HEADQUARTERS).toArray(RobotInfo[]::new);
        if (enemies.length > 0) {
            MapLocation toAttack = enemies[0].location;
            //MapLocation toAttack = rc.getLocation().add(Direction.EAST);

            if (rc.canAttack(toAttack)) {
                rc.setIndicatorString("Attacking");
                rc.attack(toAttack);
            }
            Direction dir = me.directionTo(toAttack);
            if (!at_hq) {
                if(rc.canMove(dir)) {
                    rc.move(dir);
                }
            }
        }


        //TODO: ATTACK ENEMY HEADQUARTERS!!!!!!!!!!!!!!!!
        if (at_hq) {
            return;
        }

        RobotInfo[] hqs = Arrays.stream(rc.senseNearbyRobots()).filter(robot -> robot.type == RobotType.HEADQUARTERS && robot.team != RobotPlayer.myTeam).toArray(RobotInfo[]::new);
        int min_dist = 7200;
        if (hqs.length >= 1) {
            RobotInfo closest_hq = hqs[0];
            for (RobotInfo hq: hqs) {
                int dist = hq.location.distanceSquaredTo(me);
                if (dist < min_dist) {
                    min_dist = dist;
                    closest_hq = hq;
                }
            }
            int distance_to_hq = me.distanceSquaredTo(closest_hq.location);
            if (distance_to_hq == 1 || distance_to_hq == 2){
                at_hq = true;
                return;
            }
            Direction dir = me.directionTo(closest_hq.location);
            if (rc.canMove(dir)) {
                rc.move(dir);
            }
        }

        // Also try to move *randomly*.
        Direction last_dir = directions[currentDirectionInd];
        if (rc.canMove(last_dir)) {
            rc.move(last_dir);
        } else if (rc.getMovementCooldownTurns() == 0){
            for (int i = 0; i < 3; i++) {
                currentDirectionInd = (currentDirectionInd + 2) % directions.length;
                last_dir = directions[currentDirectionInd];
                if (rc.canMove(last_dir)){
                    rc.move(last_dir);
                    break;
                }
            }
        }
    }
}
