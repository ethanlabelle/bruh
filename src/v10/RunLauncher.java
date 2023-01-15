package v10;

import battlecode.common.*;
import java.util.Arrays;
import java.util.Random;

import static v10.RobotPlayer.*;

public strictfp class RunLauncher {
    /**
     * Run a single turn for a Launcher.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
    static boolean at_hq = false;
    static boolean at_well = false;
    static boolean move_randomly = false;
    static MapLocation possibleEnemyLOC;
    static int fake_id = 0;
    static MapLocation undefined_loc = new MapLocation(-1, -1);
    static MapLocation center = new MapLocation(width/2, height/2);

    static void runLauncher(RobotController rc) throws GameActionException {
        updateMap(rc);

        attackEnemies(rc);
        
        if (at_hq || at_well) {
            return;
        }
        
        if ((rc.getRoundNum() / 150) % 2 == 0) {
            navigateTo(rc, center);
            return;
        }


        // protectWell(rc);

        if (move_randomly) {
            moveLastResort(rc);
            return;
        }
        
        if(EnemyHQLOC != null && !EnemyHQLOC.equals(undefined_loc)) {
            if (adjacentTo(rc, EnemyHQLOC)) {
                at_hq = true;
                return;
            }
            navigateTo(rc, EnemyHQLOC);
            checkForFriends(rc, EnemyHQLOC);
        }
        else{
            travelToPossibleHQ(rc);
        }
        
    }

    static void travelToPossibleHQ(RobotController rc) throws GameActionException {
        if(possibleEnemyLOC == null){
            // set possible enemy loc based on symmetry of our HQ
            int id = fake_id;
            if(id % 3 == 0)
                possibleEnemyLOC = new MapLocation(abs(spawnHQLOC.x + 1 - width), abs(spawnHQLOC.y + 1 - height));
            else if(id % 3 == 1)
                possibleEnemyLOC = new MapLocation(abs(spawnHQLOC.x + 1 - width), spawnHQLOC.y);
            else
                possibleEnemyLOC = new MapLocation(spawnHQLOC.x, abs(spawnHQLOC.y + 1 - height));
        }
        if (rc.canSenseLocation(possibleEnemyLOC)) {
            RobotInfo robot = rc.senseRobotAtLocation(possibleEnemyLOC);
            RobotInfo[] friends = rc.senseNearbyRobots(possibleEnemyLOC, 2, myTeam);
            if (robot != null && robot.getType() == RobotType.HEADQUARTERS && robot.team != RobotPlayer.myTeam && friends.length < 6) {
                EnemyHQLOC = possibleEnemyLOC;
                return;
            }
            else{
                possibleEnemyLOC = null;
                EnemyHQLOC = undefined_loc;
                fake_id += 1;
                if(fake_id == 6){
                    move_randomly = true;
                }
            }
        }
        if(possibleEnemyLOC != null)
            navigateTo(rc, possibleEnemyLOC);
    }

    static boolean adjacentTo(RobotController rc, MapLocation loc) throws GameActionException {
        int dist = rc.getLocation().distanceSquaredTo(loc);
        return dist == 1 || dist == 2;
    }

    static void attackEnemies(RobotController rc) throws GameActionException {
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
                if (rc.canAttack(toAttack)){
                    rc.attack(toAttack);
					break;
                }
            }
            // if (!at_hq && !at_well && rc.getRoundNum() % 150 == 0){
            //     navigateTo(rc, toAttack);
            // }
        }
    }

    static void protectWell(RobotController rc) throws GameActionException {
        MapLocation me = rc.getLocation();

        if (nearbyWells.length >= 1) {
            WellInfo closest_well = nearbyWells[0];
            int closest_dist = 7200;
            for(WellInfo well: nearbyWells) {
                int dist = me.distanceSquaredTo(well.getMapLocation());
                if(dist < closest_dist){
                    closest_dist = dist;
                    closest_well = well;
                }
            }
            boolean friend_already_at_well = false;
            if (rc.canSenseLocation(closest_well.getMapLocation())) {
                RobotInfo robot = rc.senseRobotAtLocation(closest_well.getMapLocation());
                if (robot != null && robot.team == myTeam && robot.type == RobotType.LAUNCHER) {
                    friend_already_at_well = true;
                }
            }

            if (me.equals(closest_well.getMapLocation())) {
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

    static void checkForFriends(RobotController rc, MapLocation hq_loc) throws GameActionException {
        if(rc.canSenseLocation(hq_loc)){
            RobotInfo[] friends = rc.senseNearbyRobots(hq_loc, 2, myTeam);
            if(friends.length >= 7){
                EnemyHQLOC = undefined_loc;
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
