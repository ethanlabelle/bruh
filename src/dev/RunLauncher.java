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
    static boolean at_well = false;
    static boolean move_randomly = false;
    static MapLocation possibleEnemyLOC;
    static int fake_id = 0;
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
    static MapLocation[] possibleHQLocs = new MapLocation[3 * GameConstants.MAX_STARTING_HEADQUARTERS];
    static int currentPredictedHQIndex = 0;
    static boolean verticalSymmetry = true;
    static boolean horizontalSymmetry = true;
    static boolean rotationalSymmetry = true;

    static void runLauncher(RobotController rc) throws GameActionException {
        attackEnemies(rc);
        if (turnCount != 1)
            updateMap(rc);
        
        // attack enemy islands
        attackEnemyIsland(rc);

        // leaves after healing
        healingStrategy(rc);

        if (isHealing && healingIsland != null) {
            attackEnemies(rc);
            cloudShot(rc);
            // Communication.clearObsoleteEnemies(rc);
            Communication.clearOld();
            return;
        }
        
		// look for targets to defend
        // if (turnCount > 50) {
            defLoc = Communication.getClosestEnemy(rc);
            if (defLoc != null) {
                Pathing.navigateTo(rc, defLoc);
                attackEnemies(rc);
                cloudShot(rc);
                Communication.clearObsoleteEnemies(rc);
                Communication.clearOld();
                return;
            }
        // }

        if (move_randomly) {
            moveLastResort(rc);
            attackEnemies(rc);
            cloudShot(rc);
            // Communication.clearObsoleteEnemies(rc);
            Communication.clearOld();
            return;
        }

        // want to stop 'outside' HQ action radius
        if(EnemyHQLOC != null && !EnemyHQLOC.equals(Pathing.undefined_loc)) {
            if (!justOutside(rc.getLocation(), EnemyHQLOC, RobotType.HEADQUARTERS.actionRadiusSquared, 20)) {
                Pathing.navigateTo(rc, EnemyHQLOC);
                checkForFriends(rc, EnemyHQLOC);
            }
        } else {
            travelToPossibleHQ(rc);
        }

        attackEnemies(rc);
        cloudShot(rc);
        avoidHQ(rc);
        // Communication.clearObsoleteEnemies(rc);
        Communication.clearOld();
    }

    static void runFollower(RobotController rc, MapLocation leader_loc) throws GameActionException {
        Pathing.navigateTo(rc, leader_loc);
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
        /*
         * INSPO
         * 
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
         */
        int symmetries = Communication.getSymmetry(rc);

        if (verticalSymmetry) {
            verticalSymmetry = Communication.getBit(symmetries, 2);
            if (!verticalSymmetry) {
                possibleHQLocs[0 * 3 + 1] = null;
                possibleHQLocs[1 * 3 + 1] = null;
                possibleHQLocs[2 * 3 + 1] = null;
                possibleHQLocs[3 * 3 + 1] = null;
            }
        }

        if (horizontalSymmetry) {
            horizontalSymmetry = Communication.getBit(symmetries, 1);
            if (!horizontalSymmetry) {
                possibleHQLocs[0 * 3 + 2] = null;
                possibleHQLocs[1 * 3 + 2] = null;
                possibleHQLocs[2 * 3 + 2] = null;
                possibleHQLocs[3 * 3 + 2] = null;
            }
        }

        if (rotationalSymmetry) {
            rotationalSymmetry = Communication.getBit(symmetries, 0);
            if (!rotationalSymmetry) {
                possibleHQLocs[0 * 3] = null;
                possibleHQLocs[1 * 3] = null;
                possibleHQLocs[2 * 3] = null;
                possibleHQLocs[3 * 3] = null;
            }
        }
        if(possibleEnemyLOC == null){
            // set possible enemy loc based on symmetry of our HQ
            MapLocation closest_predicted = null;
            int min_dist = 7200;
            // System.out.println("THE SYMMETRY IS " + verticalSymmetry + " " + horizontalSymmetry + " " + rotationalSymmetry);

            // generate list of possible hqs
            int count = 0;
            for(int i = Communication.headquarterLocs.length; --i >= 0;) {
                MapLocation curr_hq = Communication.headquarterLocs[i];
                if (curr_hq == null)
                    continue;
                count++;
                MapLocation rotationalSym = new MapLocation(abs(curr_hq.x + 1 - width), abs(curr_hq.y + 1 - height));
                if (Communication.headquarterLocsSet.contains(rotationalSym))
                    rotationalSymmetry = false;
                MapLocation verticalSym = new MapLocation(abs(curr_hq.x + 1 - width), curr_hq.y);
                if (Communication.headquarterLocsSet.contains(verticalSym))
                    verticalSymmetry = false;
                MapLocation horizontalSym = new MapLocation(curr_hq.x, abs(curr_hq.y + 1 - height));
                if (Communication.headquarterLocsSet.contains(horizontalSym))
                    horizontalSymmetry = false;
                possibleHQLocs[i * 3] = rotationalSym;
                possibleHQLocs[i * 3 + 1] = verticalSym;
                possibleHQLocs[i * 3 + 2] = horizontalSym;
                rc.setIndicatorDot(verticalSym, 255, 0, 0);
                rc.setIndicatorDot(horizontalSym, 255, 0, 0);
                rc.setIndicatorDot(rotationalSym, 255, 0, 0);
            }

            if (count > 1) {
                int verticalLine = width / 2;
                int horizontalLine = height / 2;
                int leftOfVerticalLine = 0;
                int belowHorizontalLine = 0;
                boolean top = false;
                boolean bottom = false;
                boolean left = false;
                boolean right = false;
                for (int i = Communication.headquarterLocs.length; --i >= 0; ) {
                    if (Communication.headquarterLocs[i] != null) {
                        int x = Communication.headquarterLocs[i].x;
                        int y = Communication.headquarterLocs[i].y;
                        switch (width % 2) {
                            case 0:
                                if (x < verticalLine) {
                                    leftOfVerticalLine++;
                                    left = true;
                                }
                                else 
                                    right = true;
                                break;
                            case 1:
                                if (x < verticalLine) {
                                    leftOfVerticalLine++;
                                    left = true;
                                }
                                else if (x > verticalLine)
                                    right = true;
                                else
                                    verticalSymmetry = false;
                                break;
                        }

                        switch (height % 2) {
                            case 0:
                                if (y < horizontalLine) {
                                    belowHorizontalLine++;
                                    bottom = true;
                                }
                                else
                                    top = true;
                                break;
                            case 1:
                                if (y < horizontalLine) {
                                    belowHorizontalLine++;
                                    bottom = true;
                                }
                                else if (y > horizontalLine)
                                    top = true;
                                else
                                    horizontalSymmetry = false;
                                break;
                        }
                    }
                }
                // check if horrizontal symmetry is possible with our headquarters
                if (((leftOfVerticalLine == count || leftOfVerticalLine == 0) && top && bottom) || !horizontalSymmetry) {
                    possibleHQLocs[0 * 3 + 2] = null;
                    possibleHQLocs[1 * 3 + 2] = null;
                    possibleHQLocs[2 * 3 + 2] = null;
                    possibleHQLocs[3 * 3 + 2] = null;
                }
                // check if vertical symmetry is possible with our headquarters
                if (((belowHorizontalLine == count || belowHorizontalLine == 0) && left && right) || !verticalSymmetry) {
                    possibleHQLocs[0 * 3 + 1] = null;
                    possibleHQLocs[1 * 3 + 1] = null;
                    possibleHQLocs[2 * 3 + 1] = null;
                    possibleHQLocs[3 * 3 + 1] = null;
                }

                // check if rotational symmetry is possible with our headquarters
                if (!rotationalSymmetry) {
                    possibleHQLocs[0 * 3] = null;
                    possibleHQLocs[1 * 3] = null;
                    possibleHQLocs[2 * 3] = null;
                    possibleHQLocs[3 * 3] = null;
                }

                // eliminate symmetries based on what our HQ can see
                // HQs will report other headquarters they see
                int ind = -1;
                for (int i = possibleHQLocs.length; --i >= 0;) {
                    if (possibleHQLocs[i] != null) {
                        for (int j = Communication.headquarterLocs.length; --j >= 0; ) {
                            if (Communication.headquarterLocs[j] != null && possibleHQLocs[i].distanceSquaredTo(Communication.headquarterLocs[j]) <= RobotType.HEADQUARTERS.visionRadiusSquared) {
                                switch(i % 3) {
                                case 0:
                                    possibleHQLocs[0 * 3] = null;
                                    possibleHQLocs[1 * 3] = null;
                                    possibleHQLocs[2 * 3] = null;
                                    possibleHQLocs[3 * 3] = null;
                                    break;
                                case 1:
                                    possibleHQLocs[0 * 3 + 1] = null;
                                    possibleHQLocs[1 * 3 + 1] = null;
                                    possibleHQLocs[2 * 3 + 1] = null;
                                    possibleHQLocs[3 * 3 + 1] = null;
                                    break;
                                case 2:
                                    possibleHQLocs[0 * 3 + 2] = null;
                                    possibleHQLocs[1 * 3 + 2] = null;
                                    possibleHQLocs[2 * 3 + 2] = null;
                                    possibleHQLocs[3 * 3 + 2] = null;
                                    break;
                                }
                                break;
                            }
                        }
                    }
                }
                // find closest
                for (int i = possibleHQLocs.length; --i >= 0;) {
                    if (possibleHQLocs[i] != null && possibleHQLocs[i].distanceSquaredTo(rc.getLocation()) < min_dist) {
                        closest_predicted = possibleHQLocs[i];
                        min_dist = possibleHQLocs[i].distanceSquaredTo(rc.getLocation());
                        ind = i;
                    }
                }
                possibleEnemyLOC = closest_predicted;
                currentPredictedHQIndex = ind;
                possibleHQLocs[ind] = null;
            } else {
                possibleEnemyLOC = possibleHQLocs[0];
                possibleHQLocs[0] = null;
            }
        }

        if (possibleEnemyLOC != null) {


            Pathing.navigateTo(rc, possibleEnemyLOC);
            if (rc.canSenseLocation(possibleEnemyLOC)) {
                RobotInfo robot = rc.senseRobotAtLocation(possibleEnemyLOC);
                if (robot != null && robot.getType() == RobotType.HEADQUARTERS && robot.team != RobotPlayer.myTeam) {
                    EnemyHQLOC = possibleEnemyLOC;
                    possibleEnemyLOC = null;
                    return;
                } else {
                    // remove symmetry
                    switch (currentPredictedHQIndex % 3) {
                        // rotational
                        case 0:
                            Communication.setImpossibleSymmetry(rc, Communication.ROTATIONAL_MASK);
                            possibleHQLocs[0 * 3] = null;
                            possibleHQLocs[1 * 3] = null;
                            possibleHQLocs[2 * 3] = null;
                            possibleHQLocs[3 * 3] = null;
                            break;
                        // vertical
                        case 1:
                            Communication.setImpossibleSymmetry(rc, Communication.VERTICAL_MASK);
                            possibleHQLocs[0 * 3 + 1] = null;
                            possibleHQLocs[1 * 3 + 1] = null;
                            possibleHQLocs[2 * 3 + 1] = null;
                            possibleHQLocs[3 * 3 + 1] = null;
                            break;
                        // horizontal
                        case 2:
                            Communication.setImpossibleSymmetry(rc, Communication.HORIZONTAL_MASK);
                            possibleHQLocs[0 * 3 + 2] = null;
                            possibleHQLocs[1 * 3 + 2] = null;
                            possibleHQLocs[2 * 3 + 2] = null;
                            possibleHQLocs[3 * 3 + 2] = null;
                            break;
                    }
                    MapLocation closest_predicted = null;
                    int min_dist = 7200;
                    // pick closest HQ that is outside of known HQs' action radius
                    int ind = -1;
                    // find closest
                    for (int i = possibleHQLocs.length; --i >= 0;) {
                        if (possibleHQLocs[i] != null && possibleHQLocs[i].distanceSquaredTo(rc.getLocation()) < min_dist) {
                            closest_predicted = possibleHQLocs[i];
                            min_dist = possibleHQLocs[i].distanceSquaredTo(rc.getLocation());
                            ind = i;
                        }
                    }
                    if (ind == -1) {
                        move_randomly = true;
                    } else {
                        possibleEnemyLOC = closest_predicted;
                        currentPredictedHQIndex = ind;
                        possibleHQLocs[ind] = null;
                    }
                }
            }
        }
    }
    
    static boolean adjacentTo(RobotController rc, MapLocation loc) throws GameActionException {
        int dist = rc.getLocation().distanceSquaredTo(loc);
        return dist == 1 || dist == 2;
    }

    // the new attackEnemies function uses less bytecode
    // IMPORTANT: make sure enemies list is current before call
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
                Pathing.tryMove(rc, Pathing.oppositeDirection(rc.getLocation().directionTo(toAttack)));
            }
            // if we didn't shoot, move towards the enemy of lowest health and attack it
            else if (!shot && enemies.length > 0 && rc.getActionCooldownTurns() == 0) {
                toAttack = enemies[enemies.length - 1].location;
                Pathing.tryMove(rc, rc.getLocation().directionTo(toAttack));
                if (rc.canAttack(toAttack)) {
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
            Pathing.navigateTo(rc, enemyIsland);
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
        Direction last_dir = Pathing.currentDirection;
        if (rc.canMove(last_dir)) {
            rc.move(last_dir);
        } else if (rc.getMovementCooldownTurns() == 0) {
            for (int i = 0; i < 3; i++) {
                Pathing.currentDirection = Pathing.currentDirection.rotateRight().rotateRight().rotateRight();
                last_dir = Pathing.currentDirection;
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
                EnemyHQLOC = Pathing.undefined_loc;
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
            return;
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
                Pathing.navigateTo(rc, healingIsland);
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
        rc.setIndicatorString("cloud shot");
        if (rc.isActionReady()) {
            rc.setIndicatorString("head empty");
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
                    if (rc.canAttack(clouds[i])) {
                        rc.attack(clouds[i]);
                        return;
                    }
                }
            } else if (turnCount % 3 != 0) {
                MapLocation attackLoc;
                switch(Pathing.currentDirection) {
                    case NORTHWEST:
                    case SOUTHWEST:
                    case NORTHEAST:
                    case SOUTHEAST:
                        attackLoc = rc.getLocation().add(Pathing.currentDirection).add(Pathing.currentDirection);
                        break;
                    default:
                        attackLoc = rc.getLocation().add(Pathing.currentDirection).add(Pathing.currentDirection).add(Pathing.currentDirection);
                }
                if (rc.canAttack(attackLoc)) {
                    rc.attack(attackLoc);
                }
            }
            
            // else {
            //     MapLocation attackLoc;
            //     switch(Pathing.currentDirection) {
            //         case NORTHWEST:
            //         case SOUTHWEST:
            //         case NORTHEAST:
            //         case SOUTHEAST:
            //             attackLoc = rc.getLocation().add(Pathing.currentDirection).add(Pathing.currentDirection);
            //             break;
            //         default:
            //             attackLoc = rc.getLocation().add(Pathing.currentDirection).add(Pathing.currentDirection).add(Pathing.currentDirection);
            //     }
            //     if (rc.canAttack(attackLoc)) {
            //         rc.attack(attackLoc);
            //     }
            // }
        } else {
            rc.setIndicatorString("not action ready");
        }
    }

    static void avoidHQ(RobotController rc) throws GameActionException {
        if (rc.isMovementReady()) {
            enemies = rc.senseNearbyRobots(-1, enemyTeam);
            me = rc.getLocation();
            if (enemies != null && enemies.length > 0) {
                int i = enemies.length;
                while (--i >= 0) {
                    RobotInfo robot = enemies[i];
                    if (robot.getType() == RobotType.HEADQUARTERS) {
                        Pathing.tryMove(rc, me.directionTo(robot.location).opposite());
                        break;
                    }
                }
            }
        }
    }
}
