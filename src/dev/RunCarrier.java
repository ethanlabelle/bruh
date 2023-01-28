package dev;

import battlecode.common.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.LinkedList;

import static dev.RobotPlayer.*;

public strictfp class RunCarrier {
    static RobotInfo[] enemyRobots;
    static MapLocation me;
    static final int BAN_LIST_SIZE = 10;
    static MapLocation[] bannedWells = new MapLocation[BAN_LIST_SIZE];
    static int banCounter = 0;
    static boolean foundWell = false;
    static final int CARRIER_DIFF_MOD = 4;
    static List<MapLocation> bfsQ = new LinkedList<>();
    static MapLocation exploreGoal;
    static boolean earlyAda = false;
    static boolean earlyMana = false;

    /**
     * Run a single turn for a Carrier.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
	static void runCarrier(RobotController rc) throws GameActionException {
        if (rc.getRoundNum() == 2) {
            earlyAda = true;
        } else if (rc.getRoundNum() == 3 && !earlyAda) {
            earlyMana = true;
        }

        updateMap(rc);
        Communication.clearObsoleteEnemies(rc);

        me = rc.getLocation();
        enemyRobots = rc.senseNearbyRobots(-1, rc.getTeam().opponent());


        // report enemy launchers 
		if (enemyRobots.length > 0) {
            for (RobotInfo robot: enemyRobots) {
                if (robot.type == RobotType.LAUNCHER) {
			        Communication.reportEnemy(rc, rc.getLocation());
                }
            }
		}

        if (rc.getAnchor() != null) {
            carryAnchor(rc);
            return;
        }
		
        MapLocation pWellLoc;
		if (!earlyMana && (rc.getID() % CARRIER_DIFF_MOD == 0 || earlyAda)) {
		    pWellLoc = Communication.getClosestWell(rc, ResourceType.ADAMANTIUM);
		} 
		else {
			pWellLoc = Communication.getClosestWell(rc, ResourceType.MANA);
		}
        if (wellLoc == null && pWellLoc != null && !onBanList(pWellLoc)) {
            wellLoc = pWellLoc;
        }

        foundWell = false;
		// find resources
        if (wellLoc != null && !rc.canCollectResource(wellLoc, -1) && getTotalResources(rc) < 40) {
            navigateTo(rc, wellLoc);
        }

        // Try to gather from assigned well.
		if (wellLoc != null && rc.canCollectResource(wellLoc, -1)) {
            mine(rc);
		} else if (wellLoc != null && me.distanceSquaredTo(wellLoc) <= 8 && getTotalResources(rc) < 40) {
            rc.setIndicatorString(wellLoc + " is full? " + isWellFull(rc, wellLoc) + " " + banCounter);
            if (isWellFull(rc, wellLoc)) {
                bannedWells[banCounter] = wellLoc;
                wellLoc = null;
                banCounter = ++banCounter % BAN_LIST_SIZE;
            }
        }

        // If at a well, keep collecting until full.
        if (foundWell && getTotalResources(rc) < 40) {
            return;
        }


		// try to deposite resources
        if (getTotalResources(rc) == 40) {
            HQLOC = Communication.getClosestHeadquarters(rc);
            navigateTo(rc, HQLOC);
            // try to transfer ADAMANTIUM
            if (rc.canTransferResource(HQLOC, ResourceType.ADAMANTIUM, rc.getResourceAmount(ResourceType.ADAMANTIUM))) {
                rc.transferResource(HQLOC, ResourceType.ADAMANTIUM, rc.getResourceAmount(ResourceType.ADAMANTIUM));
            }
            // try to transfer MANA
            if (rc.canTransferResource(HQLOC, ResourceType.MANA, rc.getResourceAmount(ResourceType.MANA))) {
                rc.transferResource(HQLOC, ResourceType.MANA, rc.getResourceAmount(ResourceType.MANA));
            }
            Clock.yield();
            if (rc.canTakeAnchor(HQLOC, Anchor.STANDARD)) {
                rc.takeAnchor(HQLOC, Anchor.STANDARD);
            }
        } else if (wellLoc == null) {
            if (rc.getRoundNum() < 100)
        	    exploreBFS(rc);
            else {
                // Move randomly
                Direction dir = currentDirection;
                if (rc.canMove(dir)) {
                    rc.move(dir);
                } else if (rc.getMovementCooldownTurns() == 0) {
                    currentDirection = directions[rng.nextInt(directions.length)];
                }
            }
        }
    }

    static void carrierAttack(RobotController rc) throws GameActionException {
        if (rc.canAttack(enemyRobots[0].location) && enemyRobots[0].getType() == RobotType.LAUNCHER) {
            int before = getTotalResources(rc);
            rc.attack(enemyRobots[0].location);
            rc.setIndicatorString("Attacking! before: " + before + " after: " + getTotalResources(rc));
        }
    }

    static void runAway(RobotController rc) throws GameActionException {
		for (RobotInfo r : enemyRobots) {
			if (r.type == RobotType.LAUNCHER) {
				tryMove(rc, oppositeDirection(me.directionTo(r.location)));
				tryMove(rc, oppositeDirection(me.directionTo(r.location)));
				break;
			}
		}
    }
    
    static void carryAnchor(RobotController rc) throws GameActionException {
        // If I have an anchor singularly focus on getting it to the first island I see
        int[] islands = rc.senseNearbyIslands();
        for (int id : islands) {
            MapLocation[] thisIslandLocs = rc.senseNearbyIslandLocations(id);
			if (thisIslandLocs.length > 0) {
				Team team = rc.senseTeamOccupyingIsland(id);
				MapLocation islLoc = null;
            	if (team == Team.NEUTRAL) {
					boolean onIsland = false;
            	    for (MapLocation loc: thisIslandLocs) {
						if (me.equals(loc))
							onIsland = true;
						islLoc = loc;
            	    }
					if (onIsland && rc.canPlaceAnchor()) {
            	       	rc.setIndicatorString("Huzzah, placed anchor!");
            	       	rc.placeAnchor();
            	       	Clock.yield();
            	       	Communication.updateIslandInfo(rc, id);
            	       	return;
					
					} else {
						navigateTo(rc, islLoc);
					}
            	}
            	else if (team == enemyTeam) {
            	    boolean onIsland = false;
            	    for (MapLocation loc: thisIslandLocs) {
						if (me.equals(loc))
							onIsland = true;
						islLoc = loc;
            	    }
            	    
            	    if (onIsland) return;
            	}
			}
        }

        MapLocation neutralIslandLoc = null;
        int id;
        for (id = 1; id <= GameConstants.MAX_NUMBER_ISLANDS; id++) {
			Team team = Communication.readTeamHoldingIsland(rc, id);
			MapLocation islLoc = Communication.readIslandLocation(rc, id);
			if (team.equals(Team.NEUTRAL) && islLoc != null) {
                neutralIslandLoc = Communication.readIslandLocation(rc, id);
               	break;
            }
        }
        if (neutralIslandLoc != null) {
            navigateTo(rc, neutralIslandLoc);
            for (int i: islands) {
                if (i == id) {
                    MapLocation[] thisIslandLocs = rc.senseNearbyIslandLocations(id);
					if (thisIslandLocs.length > 0) {
			        	Team team = rc.senseTeamOccupyingIsland(id);
                    	if (team == Team.NEUTRAL) {
                    	    for (MapLocation loc: thisIslandLocs) {
                    	        if (me.equals(loc) && rc.canPlaceAnchor()) {
                    	            rc.setIndicatorString("Huzzah, placed anchor!");
                    	            rc.placeAnchor();
                    	            Clock.yield();
                    	            Communication.updateIslandInfo(rc, id);
                    	            return;
                    	        }
                    	    }
                    	}
					}
                }
            }
        }
        else {
	        // try to move *randomly*.
	        Direction dir = currentDirection;
	        if (rc.canMove(dir)) {
	            rc.move(dir);
	        } else if (rc.getMovementCooldownTurns() == 0) {
	            currentDirection = directions[rng.nextInt(directions.length)];
	        }
        }
    }

    static boolean onBanList(MapLocation wellLoc) throws GameActionException {
        for (int i = 0; i < bannedWells.length; i++) {
            if (bannedWells[i] != null && bannedWells[i].equals(wellLoc))
                return true;
        }
        return false;
    }

    static void mine(RobotController rc) throws GameActionException {
		rc.collectResource(wellLoc, -1);
        rc.setIndicatorString("Collecting, now have, AD:" +
                rc.getResourceAmount(ResourceType.ADAMANTIUM) +
                " MN: " + rc.getResourceAmount(ResourceType.MANA) +
                " EX: " + rc.getResourceAmount(ResourceType.ELIXIR));
        foundWell = true;
        // if there are too many carriers about this well, forget and ban well
        if (getTotalResources(rc) == 40) {
            return;
        }
        for (int i = 0; i < directions.length; i++) {
            MapLocation miningLoc = wellLoc.add(directions[i]);
			if (!rc.onTheMap(miningLoc))
				continue;
            if (!miningLoc.equals(me) && board[miningLoc.x][miningLoc.y] != M_STORM && rc.canSenseLocation(miningLoc) && rc.senseMapInfo(miningLoc).getCurrentDirection() == Direction.CENTER && rc.canMove(me.directionTo(miningLoc))) {
                rc.move(me.directionTo(miningLoc));
                return;
            }
        } 
        if (rc.canMove(me.directionTo(wellLoc))) {
            rc.move(me.directionTo(wellLoc));
        }
    }

    static boolean isWellFull(RobotController rc, MapLocation well) throws GameActionException {
        int spots = 0;
        int taken = 0;
        for (int i = 0; i < directions.length; i++) {
            MapLocation miningLoc = wellLoc.add(directions[i]);
			if (!rc.onTheMap(miningLoc))
				continue;
            // good tile for mining
            if (board[miningLoc.x][miningLoc.y] != M_STORM && rc.canSenseLocation(miningLoc) && rc.senseMapInfo(miningLoc).getCurrentDirection() == Direction.CENTER) {
                spots++;
            } else {
                continue;
            }
            if (rc.canSenseRobotAtLocation(miningLoc))
                taken++;
        } 
        return spots == taken; 
    }

    static void exploreBFS(RobotController rc) throws GameActionException {
        if (exploreGoal == null)
            exploreGoal = me;
		else
       		navigateTo(rc, exploreGoal);
        if (me.distanceSquaredTo(exploreGoal) <= 1) {
            getUnexploredTiles(rc);
            while (bfsQ.size() > 0 && (exploreGoal == null || board[exploreGoal.x][exploreGoal.y] != 0)) {
                exploreGoal = bfsQ.remove(0);
        	    rc.setIndicatorDot(exploreGoal, 255, 0, 0);
        	}
       		navigateTo(rc, exploreGoal);
		}
    }

    static void getUnexploredTiles(RobotController rc) throws GameActionException {
		int rad = 7;
        int[] coord = {me.x + rad, me.y + rad, me.x - rad, me.y - rad, me.x + rad, me.y - rad, me.x - rad, me.y + rad};
        try {
            if (board[coord[0]][coord[1]] == 0)
                bfsQ.add(new MapLocation(coord[0], coord[1]));
        } catch (ArrayIndexOutOfBoundsException e) {}
        try {
            if (board[coord[4]][coord[5]] == 0)
                bfsQ.add(new MapLocation(coord[4], coord[5]));
        } catch (ArrayIndexOutOfBoundsException e) {}
        try {
            if (board[coord[2]][coord[3]] == 0)
                bfsQ.add(new MapLocation(coord[2], coord[3]));
        } catch (ArrayIndexOutOfBoundsException e) {}
        try {
            if (board[coord[6]][coord[7]] == 0)
                bfsQ.add(new MapLocation(coord[6], coord[7]));
        } catch (ArrayIndexOutOfBoundsException e) {}
    }
}
