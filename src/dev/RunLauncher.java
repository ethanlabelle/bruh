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
    static boolean move_randomly = false;
    static MapLocation possibleEnemyLOC;
    static int fake_id = 0;
    static MapLocation undefined_loc = new MapLocation(-1, -1);
    static MapLocation center = new MapLocation(width/2, height/2);

    static void runLauncher(RobotController rc) throws GameActionException {
        updateMap(rc);
        
        attackEnemies(rc);
        
        if ((rc.getRoundNum() / 150) % 2 == 0) {
            if (rc.getLocation().distanceSquaredTo(HQLOC) < 35)
                navigateTo(rc, center);
            return;
        }

		// look for targets to defend
		MapLocation defLoc = Communication.getClosestEnemy(rc);
		if (defLoc != null) {
			navigateTo(rc, defLoc);
            Communication.clearObsoleteEnemies(rc);
            attackEnemies(rc);
            return;
		}

        if (move_randomly) {
            moveLastResort(rc);
            return;
        }
        
        // want to stop 'outside' HQ action radius
        if(EnemyHQLOC != null && !EnemyHQLOC.equals(undefined_loc)) {
            if (justOutside(rc.getLocation(), EnemyHQLOC, RobotType.HEADQUARTERS.actionRadiusSquared, 20)) {
                return;
            }
            navigateTo(rc, EnemyHQLOC);
            checkForFriends(rc, EnemyHQLOC);
        } else{
            travelToPossibleHQ(rc);
        }

        attackEnemies(rc);
        
    }

    static void travelToPossibleHQ(RobotController rc) throws GameActionException {
        if(possibleEnemyLOC == null){
            // set possible enemy loc based on symmetry of our HQ
            int id = fake_id;
            if(id % 3 == 0)
                possibleEnemyLOC = new MapLocation(abs(HQLOC.x + 1 - width), abs(HQLOC.y + 1 - height));
            else if(id % 3 == 1)
                possibleEnemyLOC = new MapLocation(abs(HQLOC.x + 1 - width), HQLOC.y);
            else
                possibleEnemyLOC = new MapLocation(HQLOC.x, abs(HQLOC.y + 1 - height));
        }
        if (rc.canSenseLocation(possibleEnemyLOC)) {
            RobotInfo robot = rc.senseRobotAtLocation(possibleEnemyLOC);
            RobotInfo[] friends = rc.senseNearbyRobots(possibleEnemyLOC, -1, myTeam);
            if (robot != null && robot.getType() == RobotType.HEADQUARTERS && robot.team != RobotPlayer.myTeam && friends.length < 3) {
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
		boolean shot = false;
		MapLocation toAttack = null;
        if (enemies.length > 0) {
            toAttack = enemies[0].location;
            for (RobotInfo enemy: enemies) {
                toAttack = enemy.location;
                if (rc.canAttack(toAttack)){
                    rc.attack(toAttack);
					shot = true;
					break;
                }
            }
        }
		if (shot) {
			tryMove(rc, oppositeDirection(rc.getLocation().directionTo(toAttack)));
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
        Direction last_dir = currentDirection;
        if (rc.canMove(last_dir)) {
            rc.move(last_dir);
        } else if (rc.getMovementCooldownTurns() == 0) {
            for (int i = 0; i < 3; i++) {
                currentDirection = currentDirection.rotateRight().rotateRight().rotateRight();
                last_dir = currentDirection;
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
            if(friends.length >= 4){
                EnemyHQLOC = undefined_loc;
            }
        }
    }

    static boolean justOutside (MapLocation loc1, MapLocation loc2, int distanceSquared, double epsilon) {
        return distanceSquared + epsilon > loc1.distanceSquaredTo(loc2);
    }
}
