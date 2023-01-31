package v19;

import battlecode.common.*;
import java.util.Arrays;
import java.util.HashSet;

import static v19.RobotPlayer.*;

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
    static final int minimum_health = RobotType.LAUNCHER.health/2;
    static final int maximum_health = RobotType.LAUNCHER.health;
    static boolean isHealing = false;
    static MapLocation healingIsland = null;
    static MapLocation enemyIsland = null;
    static Direction oscillatDirection = directions[rng.nextInt(directions.length)];
    static RobotInfo[] friends;
    static RobotInfo[] enemies;
    static MapLocation me;
    static MapLocation defLoc;
    static boolean swarm = false;
    static HashSet<MapLocation> possibleHQLocs = new HashSet<>();

    static void runLauncher(RobotController rc) throws GameActionException {
        attackEnemies(rc);
        if (turnCount != 1)
            updateMap(rc);
        else
            HQLOC = rc.getLocation();
        
        avoidHQ(rc);
        
        if (getEnemies(rc).length > 0) {
            Communication.reportEnemy(rc, rc.getLocation());
        }
        // attack enemy islands
        attackEnemyIsland(rc);

        // leaves after healing
        healingStrategy(rc);

        if (isHealing && healingIsland != null) {
            attackEnemies(rc);
            cloudShot(rc);
            return;
        }
        
		// look for targets to defend
        
		defLoc = Communication.getClosestEnemy(rc);
		if (defLoc != null) {
			navigateTo(rc, defLoc);
            attackEnemies(rc);
            cloudShot(rc);
            return;
		}

        if (move_randomly) {
            moveLastResort(rc);
            cloudShot(rc);
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

        avoidHQ(rc);
        cloudShot(rc);
        Communication.clearOld();
    }

    static void runFollower(RobotController rc, MapLocation leader_loc) throws GameActionException {
        navigateTo(rc, leader_loc);
    }

    static MapLocation getLeader(RobotController rc) throws GameActionException {
        friends = getNearbyTeamLaunchers(rc);
        int smallest_id = rc.getID();
        MapLocation leader_loc = rc.getLocation();
        for (int i = friends.length; --i >= 0;) {
            if (friends[i].ID < smallest_id) {
                smallest_id = friends[i].ID;
                leader_loc = friends[i].location;
            }
        }
        return leader_loc;
    }

    static void travelToPossibleHQ(RobotController rc) throws GameActionException {
        if(possibleEnemyLOC == null){
            // set possible enemy loc based on symmetry of our HQ
            MapLocation closest_predicted = null;
            int min_dist = 7200;
            // me = rc.getLocation();
            // if (width*height < 1000 || rc.getID() % 2 == 0) {
            //     MapLocation curr_hq = HQLOC;
            //     closest_predicted = new MapLocation(abs(curr_hq.x + 1 - width), abs(curr_hq.y + 1 - height));
            // } else {
            // System.out.println(Communication.getClosestEnemy(rc));
            for(int i = Communication.headquarterLocs.length; --i >= 0;) {
                MapLocation curr_hq = Communication.headquarterLocs[i];
                if (curr_hq == null)
                    continue;
                MapLocation rotationalSym = new MapLocation(abs(curr_hq.x + 1 - width), abs(curr_hq.y + 1 - height));
                // MapLocation verticalSym = new MapLocation(abs(HQLOC.x + 1 - width), HQLOC.y);
                // MapLocation horizontalSym = new MapLocation(HQLOC.x, abs(HQLOC.y + 1 - height));
                // possibleHQLocs.add(rotationalSym);
                // possibleHQLocs.add(verticalSym);
                // possibleHQLocs.add(horizontalSym);
                // guess on rotational symmetry
                if (HQLOC.distanceSquaredTo(rotationalSym) < min_dist && !Communication.headquarterLocsSet.contains(rotationalSym)) {
                    min_dist = HQLOC.distanceSquaredTo(rotationalSym);
                    closest_predicted = rotationalSym;
                }
                // if (HQLOC.distanceSquaredTo(verticalSym) < min_dist && !Communication.headquarterLocsSet.contains(verticalSym)) {
                //     min_dist = HQLOC.distanceSquaredTo(verticalSym);
                //     closest_predicted = verticalSym;
                // }
                // if (HQLOC.distanceSquaredTo(horizontalSym) < min_dist && !Communication.headquarterLocsSet.contains(horizontalSym)) {
                //     min_dist = HQLOC.distanceSquaredTo(horizontalSym);
                //     closest_predicted = horizontalSym;
                // }
            }
            possibleEnemyLOC = closest_predicted;
            // if (rc.canSenseLocation(possibleEnemyLOC)) {
            //     RobotInfo robot = rc.senseRobotAtLocation(possibleEnemyLOC);
            //     RobotInfo[] friends = rc.senseNearbyRobots(possibleEnemyLOC, -1, myTeam);
            //     if (robot != null && robot.getType() == RobotType.HEADQUARTERS && robot.team != RobotPlayer.myTeam && friends.length < 3) {
            //         EnemyHQLOC = possibleEnemyLOC;
            //         return;
            //     }
            //     else{
            //         possibleEnemyLOC = null;
            //         EnemyHQLOC = undefined_loc;
            //         fake_id += 1;
            //         if(fake_id == 6){
            //             move_randomly = true;
            //         }
            //     }
            // }
        }

        if (possibleEnemyLOC != null) {
            navigateTo(rc, possibleEnemyLOC);
            if (rc.canSenseLocation(possibleEnemyLOC)) {
                RobotInfo robot = rc.senseRobotAtLocation(possibleEnemyLOC);
                if (robot != null && robot.getType() == RobotType.HEADQUARTERS && robot.team != RobotPlayer.myTeam) {
                    EnemyHQLOC = possibleEnemyLOC;
                    possibleEnemyLOC = null;
                    return;
                } else {
                    move_randomly = true;
                }
            }
        }
    }

    static boolean adjacentTo(RobotController rc, MapLocation loc) throws GameActionException {
        int dist = rc.getLocation().distanceSquaredTo(loc);
        return dist == 1 || dist == 2;
    }

    // the new attackEnemies function uses less bytecode
    static void attackEnemies(RobotController rc) throws GameActionException {
        if (rc.isActionReady()) {
            enemies = getEnemies(rc);
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
            me = rc.getLocation();
            short islandNum = myTeam == Team.A ? M_AISL : M_BISL;
            if (board[me.x + me.y * width] == islandNum || board[me.x + me.y * width] == M_NISL) {
                enemyIsland = null;
                return;
            }
            navigateTo(rc, enemyIsland);
        }
    }

    static void protectWell(RobotController rc) throws GameActionException {
        me = rc.getLocation();

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
                me = rc.getLocation();
                short islandNum = myTeam == Team.A ? M_AISL : M_BISL;
                if (board[me.x + me.y * width] == islandNum) {
                    if (rc.canSenseLocation(healingIsland)) {
                        int islandId = rc.senseIsland(healingIsland);
                        MapLocation[] islandLocs = rc.senseNearbyIslandLocations(islandId);
                        for (MapLocation islandLoc : islandLocs) {
                            if (rc.canMove(me.directionTo(islandLoc))) {
                                rc.move(me.directionTo(islandLoc));
                                return;
                            }
                        }
                        return;
                    }
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

    static RobotInfo[] getNearbyTeamLaunchers(RobotController rc) throws GameActionException {
        RobotInfo[] nearbyTeamLaunchers = robotInfos;
        int n = 0;
        for (int i = nearbyTeamLaunchers.length; --i >= 0;) {
            if (nearbyTeamLaunchers[i].type == RobotType.LAUNCHER && nearbyTeamLaunchers[i].team == myTeam) {
                n++;
            }
        }
        RobotInfo[] teamLaunchers = new RobotInfo[n];
        for (int i = nearbyTeamLaunchers.length; --i >= 0;) {
            if (nearbyTeamLaunchers[i].type == RobotType.LAUNCHER && nearbyTeamLaunchers[i].team == myTeam) {
                teamLaunchers[--n] = nearbyTeamLaunchers[i];
            }
        }
        return teamLaunchers;
    }
    
    static void cloudShot(RobotController rc) throws GameActionException {
        if (rc.isActionReady()) {
            enemies = getEnemies(rc);
            if (enemies.length == 0) {
                if (!rc.senseCloud(rc.getLocation())) {
                    // sense nearby clouds and attack a location in there
                    MapLocation[] clouds = rc.senseNearbyCloudLocations();
                    for (int i = clouds.length; --i >= 0;) {
                        int j;
                        if (i == 0) {
                            j = i;
                        } else {
                            j = rng.nextInt(i);
                        }
                        if (!(rc.getLocation().distanceSquaredTo(clouds[j]) <= 4) && rc.canAttack(clouds[j])) {
                            rc.attack(clouds[j]);
                            return;
                        }
                    }
                    for (int i = clouds.length; --i >= 0;) {
                        if (!(rc.getLocation().distanceSquaredTo(clouds[i]) <= 4) && rc.canAttack(clouds[i])) {
                            rc.attack(clouds[i]);
                            return;
                        }
                    }
                } else {
                    MapLocation attackLoc;
                    switch(currentDirection) {
                        case NORTHWEST:
                        case SOUTHWEST:
                        case NORTHEAST:
                        case SOUTHEAST:
                            attackLoc = rc.getLocation().add(currentDirection).add(currentDirection);
                            break;
                        default:
                            attackLoc = rc.getLocation().add(currentDirection).add(currentDirection).add(currentDirection);
                    }
                    if (rc.canAttack(attackLoc)) {
                        rc.attack(attackLoc);
                    }
                }
            }
        }
    }

    static void avoidHQ(RobotController rc) throws GameActionException {
        enemies = rc.senseNearbyRobots(-1, enemyTeam);
        me = rc.getLocation();
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
}
