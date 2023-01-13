package v8;

import battlecode.common.*;
import java.util.Arrays;
import java.util.Random;

import static v8.RobotPlayer.*;

public strictfp class RunLauncher {
    /**
     * Run a single turn for a Launcher.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
    static boolean at_hq = false;
    static boolean at_well = false;

    static void runLauncher(RobotController rc) throws GameActionException {
        updateMap(rc);
        // Try to attack someone
        Team opponent = rc.getTeam().opponent();
        MapLocation me = rc.getLocation();
        RobotInfo[] enemies = Arrays.stream(rc.senseNearbyRobots(-1, opponent)).filter(robot -> robot.type != RobotType.HEADQUARTERS).toArray(RobotInfo[]::new);
        // sort by health and put launcher types first in the array
        Arrays.sort(enemies, (robot1, robot2) -> {
            if (robot1.type == RobotType.LAUNCHER && robot2.type != RobotType.LAUNCHER) {
                return -1;
            } else if (robot1.type != RobotType.LAUNCHER && robot2.type == RobotType.LAUNCHER) {
                return 1;
            } else {
                return robot1.health - robot2.health;
            }
        });
        if (enemies.length > 0) {
            MapLocation toAttack = enemies[0].location;
            for (RobotInfo enemy: enemies) {
                toAttack = enemy.location;
                if (rc.canAttack(toAttack)) {
                    rc.attack(toAttack);
                }
            }
            Direction dir = me.directionTo(toAttack);
            if (!at_hq && !at_well){
                if (rc.canMove(dir)) {
                    rc.move(dir);
                    me = rc.getLocation();
                }
            }
        }

        if (at_hq || at_well) {
            return;
        }

        RobotInfo[] nearby_robots = rc.senseNearbyRobots();
        WellInfo[] nearby_wells = rc.senseNearbyWells();

        int min_dist_well = 7200;
        if (nearby_wells.length >= 1) {
            WellInfo closest_well = nearby_wells[0];
            for (WellInfo well: nearby_wells) {
                int dist = well.getMapLocation().distanceSquaredTo(me);
                if (dist < min_dist_well) {
                    min_dist_well = dist;
                    closest_well = well;
                }
            }

            
            boolean friend_already_at_well = false;
            for (RobotInfo robot: nearby_robots) {
                int robot_dist_to_well = robot.getLocation().distanceSquaredTo(closest_well.getMapLocation());
                if (robot.team == RobotPlayer.myTeam && robot.getType() == RobotType.LAUNCHER && robot_dist_to_well == 0) {
                    friend_already_at_well = true;
                    break;
                }
            }

            int distance_to_well = me.distanceSquaredTo(closest_well.getMapLocation());
            if (!friend_already_at_well && distance_to_well == 0) {
                at_well = true;
                return;
            }
            
            if (!friend_already_at_well) {
                Direction dir = me.directionTo(closest_well.getMapLocation());
                if (rc.canMove(dir)) {
                    rc.move(dir);
                }
            }

        }

        if(EnemyHQLOC != null) {
            if (me.distanceSquaredTo(EnemyHQLOC) == 1 || me.distanceSquaredTo(EnemyHQLOC) == 2){
                at_hq = true;
                return;
            }
            navigateTo(rc, EnemyHQLOC);
        }

        else{
			MapLocation possibleEnemyLOC = new MapLocation(abs(spawnHQLOC.x - width) , abs(spawnHQLOC.y - height));
            navigateTo(rc, possibleEnemyLOC);
        }
    }
}
