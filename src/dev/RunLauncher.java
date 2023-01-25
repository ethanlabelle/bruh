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
    static final int minimum_health = 30;
    static final int maximum_health = RobotType.LAUNCHER.health / 2;
    static boolean isHealing = false;
    static MapLocation healingIsland = null;
    static MapLocation enemyIsland = null;
    static Direction oscillatDirection = directions[rng.nextInt(directions.length)];
    static MapLocation [] enemyHQs = new MapLocation [GameConstants.MAX_STARTING_HEADQUARTERS];
    static RobotInfo [] enemies;
    public static MapLocation [] verticalEnemies = null;
    public static MapLocation [] horizontalEnemies = null;
    public static MapLocation [] rotationalEnemies = null;
    public static boolean canVertical = true;
    public static boolean canHorizontal = true;
    public static boolean canRotational = true;

    static void runLauncher(RobotController rc) throws GameActionException {

        attackEnemies(rc);

        // updates to see if can eleminate one of the semetrees
        updateHorizontal();
        updateVertical();
        updateRotational();
        
        if (turnCount != 0)
            updateMap(rc);

        // attack enemy islands
        attackEnemyIsland(rc);
        
        // leaves after healing
        healingStrategy(rc);

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
            if (!justOutside(rc.getLocation(), EnemyHQLOC, RobotType.HEADQUARTERS.actionRadiusSquared, 20)) {
                navigateTo(rc, EnemyHQLOC);
                checkForFriends(rc, EnemyHQLOC);
            }
        } else {
            travelToPossibleHQ(rc);
        }

        attackEnemies(rc);

        enemies = rc.senseNearbyRobots(-1, enemyTeam);
        MapLocation me = rc.getLocation();
        if (enemies != null && enemies.length > 0) {
            int i = enemies.length;
            while (--i >= 0) {
                        RobotInfo robot = enemies[i];
                if (robot.getType() == RobotType.HEADQUARTERS) {
                                tryMove(rc, me.directionTo(robot.location).opposite());
                                break;
                }
            }
        }
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

    // the new attackEnemies function uses less bytecode
    static void attackEnemies(RobotController rc) throws GameActionException {
        enemies = getEnemies(rc);
        if (enemies.length == 0) {
            return;
        }
        // sort by descending health and put launcher types last in the array
        Arrays.sort(enemies, (robot1, robot2) -> {
            if (robot1.type == RobotType.LAUNCHER && robot2.type != RobotType.LAUNCHER) {
                return 1;
            } else if (robot1.type != RobotType.LAUNCHER && robot2.type == RobotType.LAUNCHER) {
                return -1;
            } else {
                return robot2.health - robot1.health;
            }
        });
		boolean shot = false;
		MapLocation toAttack = null;
        for (int i = enemies.length; --i >= 0;) {
            toAttack = enemies[i].location;
            if (rc.canAttack(toAttack) && enemies[i].getType() == RobotType.LAUNCHER) {
                rc.attack(toAttack);
                shot = true;
                break;
            }
        }
		if (shot) {
			tryMove(rc, oppositeDirection(rc.getLocation().directionTo(toAttack)));
		}
        // if we didn't shoot, move towards the enemy of lowest health and attack it
        else if (!shot && enemies.length > 0 && rc.getActionCooldownTurns() == 0) {
            toAttack = enemies[enemies.length - 1].location;
            tryMove(rc, rc.getLocation().directionTo(toAttack));
            if (rc.canAttack(toAttack)){
                rc.attack(toAttack);
            }
        }
    }

    static RobotInfo[] getEnemies(RobotController rc) throws GameActionException {
        RobotInfo[] enemyInfos = rc.senseNearbyRobots(-1, enemyTeam);
        int n = 0;
        for (int i = enemyInfos.length; --i >= 0;) {
            if (enemyInfos[i].type != RobotType.HEADQUARTERS) {
                n++;
            }
        }
        enemies = new RobotInfo[n];
        for (int i = enemyInfos.length; --i >= 0;) {
            if (enemyInfos[i].type != RobotType.HEADQUARTERS) {
                enemies[--n] = enemyInfos[i];
            }
        }
        return enemies;
    }


    static void attackEnemyIsland(RobotController rc) throws GameActionException {
        if (enemyIsland == null) {
            enemyIsland = getClosestEnemyIsland(rc);
        }
        if (enemyIsland != null) {
            MapLocation me = rc.getLocation();
            short islandNum = myTeam == Team.A ? M_AISL : M_BISL;
            if (board[me.x][me.y] == islandNum || board[me.x][me.y] == M_NISL) {
                enemyIsland = null;
                return;
            }
            navigateTo(rc, enemyIsland);
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

    static void healingStrategy(RobotController rc) throws GameActionException {
        // leave if healed
        if (isHealing && rc.getHealth() >= maximum_health) {
            isHealing = false;
            healingIsland = null;
        }

        // check if need to go to island
        if (isHealing || rc.getHealth() < minimum_health) {
            isHealing = true;
            if (healingIsland == null) {
                healingIsland = getClosestControlledIsland(rc);
            }
            if (healingIsland != null) {
                rc.setIndicatorString("trying to heal!!");
                MapLocation me = rc.getLocation();
                short islandNum = myTeam == Team.A ? M_AISL : M_BISL;
                if (board[me.x][me.y] == islandNum) {
                    bugRandom(rc, healingIsland);
                    return;
                }
                navigateTo(rc, healingIsland);
                return;
            }
        }
    }

    static void wiggle(RobotController  rc) throws GameActionException {
        oscillatDirection = oscillatDirection.opposite();
        if (rc.canMove(oscillatDirection)) {
            rc.move(oscillatDirection);
        }
    }
        // quick check to see if poss vertical
        if (!canVertical) {
            return;
        }
        // check to see if possible vertical enemies are found
        if (verticalEnemies == null) {
            MapLocation [] otherLoc = new MapLocation [GameConstants.MAX_STARTING_HEADQUARTERS];
            for (int index = 0; index < GameConstants.MAX_STARTING_HEADQUARTERS; index ++) {
                if (Communication.headquarterLocs[index] == null) {
                    break;
                }
            otherLoc[index] = new MapLocation(abs(Communication.headquarterLocs[index].x + 1 - width), Communication.headquarterLocs[index].y);
            }
            verticalEnemies = otherLoc;
        }
        // checks the validaty of the assessment
        MapLocation [] HQs = new MapLocation [GameConstants.MAX_STARTING_HEADQUARTERS];
        int index = 0;
        for (RobotInfo enemy : enemies) {
            if (enemy.type == RobotType.HEADQUARTERS) {
                HQs[index] = enemy.location;
                index ++;
            }
        }
        for (index = 0; index < GameConstants.MAX_STARTING_HEADQUARTERS; index ++) {
            if (HQs[index] == null) {
                break;
            }
            if (!Arrays.asList(verticalEnemies).contains(HQs[index])) {
                verticalEnemies = null;
                canVertical = false;
                break;
            }
        }
    }
    public static void updateHorizontal () {
        if (!canHorizontal) {
            return;
        }
        if (horizontalEnemies == null) {
            MapLocation [] otherLoc = new MapLocation [GameConstants.MAX_STARTING_HEADQUARTERS];
            for (int index = 0; index < GameConstants.MAX_STARTING_HEADQUARTERS; index ++) {
                if (Communication.headquarterLocs[index] == null) {
                    break;
                }
                otherLoc[index] = new MapLocation(Communication.headquarterLocs[index].x, abs(Communication.headquarterLocs[index].y + 1 - height));
            }
            horizontalEnemies = otherLoc;
        }
        MapLocation [] HQs = new MapLocation [GameConstants.MAX_STARTING_HEADQUARTERS];
        int index = 0;
        for (RobotInfo enemy : enemies) {
            if (enemy.type == RobotType.HEADQUARTERS) {
                HQs[index] = enemy.location;
                index ++;
            }
        }
        for (index = 0; index < GameConstants.MAX_STARTING_HEADQUARTERS; index ++) {
            if (HQs[index] == null) {
                break;
            }
            if (!Arrays.asList(horizontalEnemies).contains(HQs[index])) {
                horizontalEnemies = null;
                canHorizontal = false;
                break;
            }
        }
    }
    public static void updateRotational () {
        if (!canRotational) {
            return;
        }
        if (rotationalEnemies == null) {
            MapLocation [] otherLoc = new MapLocation [GameConstants.MAX_STARTING_HEADQUARTERS];
            for (int index = 0; index < GameConstants.MAX_STARTING_HEADQUARTERS; index ++) {
                if (Communication.headquarterLocs[index] == null) {
                    break;
                }
                otherLoc[index] = new MapLocation(abs(Communication.headquarterLocs[index].x + 1 - width), abs(Communication.headquarterLocs[index].y + 1 - height));
            }
            rotationalEnemies = otherLoc;
        }
        MapLocation [] HQs = new MapLocation [GameConstants.MAX_STARTING_HEADQUARTERS];
        int index = 0;
        for (RobotInfo enemy : enemies) {
            if (enemy.type == RobotType.HEADQUARTERS) {
                HQs[index] = enemy.location;
                index ++;
            }
        }
        for (index = 0; index < GameConstants.MAX_STARTING_HEADQUARTERS; index ++) {
            if (HQs[index] == null) {
                break;
            }
            if (!Arrays.asList(rotationalEnemies).contains(HQs[index])) {
                rotationalEnemies = null;
                canRotational = false;
                break;
            }
        }
    }
}
