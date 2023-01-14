package dev;

import battlecode.common.*;
import java.util.Arrays;
import java.util.Random;

import static dev.RobotPlayer.*;

public strictfp class RunLauncher {
    /**
     * Run a single turn for a Launcher.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
    static boolean at_hq = false;
    static boolean at_well = false;
    static boolean guess_was_right = true;
    static int fake_id = 0;
    static MapLocation possibleEnemyLOC = null;

    static void runLauncher(RobotController rc) throws GameActionException {
        updateMap(rc);

        attackEnemies(rc);

        if (at_hq || at_well) {
            return;
        }

        protectWell(rc);

        if(EnemyHQLOC != null) {
            if (adjacentTo(rc, EnemyHQLOC)) {
                at_hq = true;
                return;
            }
            navigateTo(rc, EnemyHQLOC);
        }

        else{
			possibleEnemyLOC = new MapLocation(abs(spawnHQLOC.x - width) , abs(spawnHQLOC.y - height));
            navigateTo(rc, possibleEnemyLOC);
        }
    }

    static void travelToPossibleHQ(RobotController rc) throws GameActionException {
        if(possibleEnemyLOC == null){
            // set possible enemy loc based on symmetry of our HQ
            int id = fake_id;
            if(id % 3 == 0)
                possibleEnemyLOC = new MapLocation(abs(spawnHQLOC.x - width), abs(spawnHQLOC.y - height));
            else if(id % 3 == 1)
                possibleEnemyLOC = new MapLocation(abs(spawnHQLOC.x - width), spawnHQLOC.y);
            else
                possibleEnemyLOC = new MapLocation(spawnHQLOC.x, abs(spawnHQLOC.y - height));
        }
        navigateTo(rc, possibleEnemyLOC);
        if (rc.canSenseLocation(possibleEnemyLOC)) {
            RobotInfo robot = rc.senseRobotAtLocation(possibleEnemyLOC);
            if (robot != null && robot.getType() == RobotType.HEADQUARTERS && robot.team != RobotPlayer.myTeam) {
                EnemyHQLOC = possibleEnemyLOC;
                return;
            }
            else{
                possibleEnemyLOC = null;
                fake_id += 1;
                if (fake_id == 3) {
                    guess_was_right = false;
                }
            }
        }
    }

    static boolean adjacentTo(RobotController rc, MapLocation loc) throws GameActionException {
        int dist = rc.getLocation().distanceSquaredTo(loc);
        return dist == 1 || dist == 2;
    }

    static void attackEnemies(RobotController rc) throws GameActionException {
        MapLocation me = rc.getLocation();
        Team opponent = RobotPlayer.myTeam.opponent();
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
    }

    static void protectWell(RobotController rc) throws GameActionException {
        RobotInfo[] nearby_robots = rc.senseNearbyRobots();
        WellInfo[] nearby_wells = rc.senseNearbyWells();
        MapLocation me = rc.getLocation();

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
    }

    static void moveLastResort(RobotController rc) throws GameActionException {
        Direction last_dir = directions[currentDirectionInd];
        if (rc.canMove(last_dir)) {
            rc.move(last_dir);
        } else if (rc.getMovementCooldownTurns() == 0) {
            for (int i = 0; i < 3; i++) {
                currentDirectionInd = (currentDirectionInd + 3) % directions.length;
                last_dir = directions[currentDirectionInd];
                if (rc.canMove(last_dir)) {
                    rc.move(last_dir);
                    break;
                }
            }
        }
    }
}

        // RobotInfo[] hqs = Arrays.stream(nearby_robots).filter(robot -> robot.type == RobotType.HEADQUARTERS && robot.team != RobotPlayer.myTeam).toArray(RobotInfo[]::new);
        // int min_dist = 7200;
        // RobotInfo closest_hq = null;
        // if (hqs.length >= 1) {
        //     closest_hq = hqs[0];
        //     for (RobotInfo hq: hqs) {
        //         int dist = hq.location.distanceSquaredTo(me);
        //         if (dist < min_dist) {
        //             min_dist = dist;
        //             closest_hq = hq;
        //         }
        //     }
        //     int distance_to_hq = me.distanceSquaredTo(closest_hq.location);
        //     if (distance_to_hq == 1 || distance_to_hq == 2){
        //         at_hq = true;
        //         return;
        //     }
        //     Direction dir = me.directionTo(closest_hq.location);
        //     if (rc.canMove(dir)) {
        //         rc.move(dir);
        //     }
        // }
