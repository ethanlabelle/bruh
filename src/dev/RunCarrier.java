package dev;

import battlecode.common.*;

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
    static final int CARRIER_DIFF_MOD = 6;
    static List<MapLocation> bfsQ = new LinkedList<>();
    static MapLocation exploreGoal;
    static boolean earlyAda = false;
    static boolean earlyMana = false;
    static int rad = 4;
    static MapLocation enemyIsland;
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
        // if (rc.getRoundNum() < 50) {
        //     earlyMana = true;
        // }
        updateMap(rc);
        Communication.clearObsoleteEnemies(rc);

        me = rc.getLocation();
        enemyRobots = rc.senseNearbyRobots(-1, rc.getTeam().opponent());

        if (enemyRobots.length > 0 && getTotalResources(rc) >= 5) {
            carrierAttack(rc);
        }

        // if (enemyRobots.length > 0 && getTotalResources(rc) < 5) {
            
        //     runAway(rc);
        // }
        if (enemyRobots.length > 0) {
            for (RobotInfo robot: enemyRobots) {
                if (robot.type == RobotType.LAUNCHER) {
			        Communication.reportEnemy(rc, robot.location);
                    if (getTotalResources(rc) < 5)
                        runAway(rc);
                    break;
                }
            }
		}
        if (rc.getAnchor() != null) {
            carryAnchor(rc);
            return;
        }
		
        // if (wellLoc == null) {
        MapLocation pWellLoc;
        if (!earlyMana && (rc.getID() % CARRIER_DIFF_MOD == 0 || earlyAda)) {
            pWellLoc = Communication.getClosestUnbannedWell(rc, ResourceType.ADAMANTIUM);
        } 
        else {
            pWellLoc = Communication.getClosestUnbannedWell(rc, ResourceType.MANA);
        }
        if (pWellLoc != null) {
            if (wellLoc == null || pWellLoc.distanceSquaredTo(HQLOC) < wellLoc.distanceSquaredTo(HQLOC))
                wellLoc = pWellLoc;
        }
        // }

        foundWell = false;
		// find resources
        if (wellLoc != null && !rc.canCollectResource(wellLoc, -1) && getTotalResources(rc) < 39) {
            if (Pathing.hasPath) {
                Pathing.navigateToWithPath(rc, wellLoc, true);
                Pathing.navigateToWithPath(rc, wellLoc, true);
            }
            else Pathing.navigateTo(rc, wellLoc);
        }

        // Try to gather from assigned well.
        if (wellLoc != null)
            rc.setIndicatorString("wellLoc " + wellLoc + " " + rc.canCollectResource(wellLoc, -1) + " "  + rc.getLocation());
		if (wellLoc != null && rc.canCollectResource(wellLoc, -1)) {
            rc.setIndicatorString("trying to mine");
            mine(rc);
		} else if (wellLoc != null && me.distanceSquaredTo(wellLoc) <= 9 && getTotalResources(rc) < 39) {
            if (isWellFull(rc, wellLoc)) {
                bannedWells[banCounter] = wellLoc;
                wellLoc = null;
                banCounter = ++banCounter % BAN_LIST_SIZE;
            }
        }

		// try to deposite resources
        if (getTotalResources(rc) >= 39) {
            HQLOC = Communication.getClosestHeadquarters(rc);
            if (Pathing.hasPath) {
                Pathing.navigateToWithPath(rc, HQLOC, false);
                Pathing.navigateToWithPath(rc, HQLOC, false);
            }
            else Pathing.navigateTo(rc, HQLOC);
            // try to transfer ADAMANTIUM
            int ada = rc.getResourceAmount(ResourceType.ADAMANTIUM);
            int mana = rc.getResourceAmount(ResourceType.MANA);

            if (ada > 0 && rc.canTransferResource(HQLOC, ResourceType.ADAMANTIUM, ada)) {
                rc.transferResource(HQLOC, ResourceType.ADAMANTIUM, ada);
            }
            // try to transfer MANA
            if (mana > 0 && rc.canTransferResource(HQLOC, ResourceType.MANA, mana)) {
                rc.transferResource(HQLOC, ResourceType.MANA, mana);
            }
            if (rc.canSenseRobotAtLocation(HQLOC)) {
                RobotInfo hqInfo = rc.senseRobotAtLocation(HQLOC);
                if (hqInfo.getNumAnchors(Anchor.STANDARD) > 0) {
                    Clock.yield();
                    turnCount++;
                    if (rc.canTakeAnchor(HQLOC, Anchor.STANDARD))
                        rc.takeAnchor(HQLOC, Anchor.STANDARD);
                }
            }
        } else if (wellLoc == null) {
            short islandNum = myTeam != Team.A ? M_AISL : M_BISL;
            if (board[me.x + me.y * width] == islandNum) {
                return;
            }  
            if (rc.getRoundNum() < 300)
                exploreBFS(rc);
            else {
                // attackEnemyIsland(rc);
                // Move randomly
                Direction dir = Pathing.currentDirection;
                if (rc.canMove(dir)) {
                    rc.move(dir);
                } else if (rc.getMovementCooldownTurns() == 0) {
                    Pathing.currentDirection = directions[rng.nextInt(directions.length)];
                }
            }
        }
        Communication.tryWriteMessages(rc);
        while (wellLoc != null && Clock.getBytecodeNum() < 10000 && !Pathing.hasPath) {
            Pathing.bfs(rc, HQLOC, wellLoc);
        }
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
				Pathing.tryMove(rc, Pathing.oppositeDirection(me.directionTo(r.location)));
				Pathing.tryMove(rc, Pathing.oppositeDirection(me.directionTo(r.location)));
				break;
			}
		}
    }
    
    static void carryAnchor(RobotController rc) throws GameActionException {
        // go to closest island!
        me = rc.getLocation();
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
            	       	Communication.updateIslandInfo(rc, id);
                        Communication.tryWriteMessages(rc);
            	       	return;
					
					} else {
						Pathing.navigateTo(rc, islLoc);
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
        int minDist = 7200;
        int id;
        for (id = 1; id <= GameConstants.MAX_NUMBER_ISLANDS; id++) {
			Team team = Communication.readTeamHoldingIsland(rc, id);
			MapLocation islLoc = Communication.readIslandLocation(rc, id);
			if (team.equals(Team.NEUTRAL) && islLoc != null && me.distanceSquaredTo(islLoc) < minDist) {
                neutralIslandLoc = Communication.readIslandLocation(rc, id);
                minDist = me.distanceSquaredTo(islLoc);
            }
        }
        if (neutralIslandLoc != null) {
            Pathing.navigateTo(rc, neutralIslandLoc);
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
                    	            Communication.updateIslandInfo(rc, id);
                                    Communication.tryWriteMessages(rc);
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
	        Direction dir = Pathing.currentDirection;
	        if (rc.canMove(dir)) {
	            rc.move(dir);
	        } else if (rc.getMovementCooldownTurns() == 0) {
	            Pathing.currentDirection = directions[rng.nextInt(directions.length)];
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
        if (getTotalResources(rc) >= 39) {
            return;
        }
        for (int i = directions.length; --i >= 0;) {
            MapLocation miningLoc = wellLoc.add(directions[i]);
			if (!rc.onTheMap(miningLoc))
				continue;
            if (!miningLoc.equals(me) && board[miningLoc.x + miningLoc.y * width] != M_STORM && rc.canSenseLocation(miningLoc) && rc.senseMapInfo(miningLoc).getCurrentDirection() == Direction.CENTER && rc.canMove(me.directionTo(miningLoc))) {
                rc.move(me.directionTo(miningLoc));
                break;
            }
        } 
        if (rc.canMove(me.directionTo(wellLoc))) {
            rc.move(me.directionTo(wellLoc));
        }
    }

    static boolean isWellFull(RobotController rc, MapLocation well) throws GameActionException {
        int spots = 1;
        int taken = 0;
        for (int i = directions.length; --i >= 0;) {
            MapLocation miningLoc = wellLoc.add(directions[i]);
			if (!rc.onTheMap(miningLoc))
				continue;
            // good tile for mining
            if (board[miningLoc.x + miningLoc.y * width] != M_STORM && rc.canSenseLocation(miningLoc) && rc.senseMapInfo(miningLoc).getCurrentDirection() == Direction.CENTER) {
                spots++;
            } else {
                continue;
            }
            if (rc.canSenseRobotAtLocation(miningLoc)) {
                RobotInfo r = rc.senseRobotAtLocation(miningLoc);
                if (r.team == myTeam)
                    taken++;
            }
        }
        if (rc.canSenseRobotAtLocation(wellLoc)) {
            RobotInfo r = rc.senseRobotAtLocation(wellLoc);
            if (r.team == myTeam)
                taken++;
        }
        return spots == taken; 
    }
    
    static void banWellLoc() {
        bannedWells[banCounter] = wellLoc;
        wellLoc = null;
        banCounter = ++banCounter % BAN_LIST_SIZE;
    }

    static void exploreBFS(RobotController rc) throws GameActionException {
        if (exploreGoal == null)
            exploreGoal = me;
		else
       		Pathing.navigateTo(rc, exploreGoal);
        if (me.distanceSquaredTo(exploreGoal) <= 1) {
            getUnexploredTiles(rc);
            while (bfsQ.size() > 0 && (exploreGoal == null || board[exploreGoal.x + exploreGoal.y * width] != 0)) {
                exploreGoal = bfsQ.remove(0);
        	    rc.setIndicatorDot(exploreGoal, 255, 0, 0);
        	}
            if (bfsQ.size() == 0) {
                rad++;
            }
       		Pathing.navigateTo(rc, exploreGoal);
		}
    }

    static void getUnexploredTiles(RobotController rc) throws GameActionException {
		// int rad = 3;
        int[] coord = {me.x + rad, me.y + rad, me.x - rad, me.y - rad, me.x + rad, me.y - rad, me.x - rad, me.y + rad};
        switch (rng.nextInt(4)) {
            case 0:
                try {
                    if (board[coord[0] + coord[1] * width] == 0)
                        bfsQ.add(new MapLocation(coord[0], coord[1]));
                } catch (ArrayIndexOutOfBoundsException e) {}
                try {
                    if (board[coord[4] + coord[5] * width] == 0)
                        bfsQ.add(new MapLocation(coord[4], coord[5]));
                } catch (ArrayIndexOutOfBoundsException e) {}
                try {
                    if (board[coord[2] + coord[3] * width] == 0)
                        bfsQ.add(new MapLocation(coord[2], coord[3]));
                } catch (ArrayIndexOutOfBoundsException e) {}
                try {
                    if (board[coord[6] + coord[7] * width] == 0)
                        bfsQ.add(new MapLocation(coord[6], coord[7]));
                } catch (ArrayIndexOutOfBoundsException e) {}
                break;
            case 1:
                try {
                    if (board[coord[4] + coord[5] * width] == 0)
                        bfsQ.add(new MapLocation(coord[4], coord[5]));
                } catch (ArrayIndexOutOfBoundsException e) {}
                try {
                    if (board[coord[2] + coord[3] * width] == 0)
                        bfsQ.add(new MapLocation(coord[2], coord[3]));
                } catch (ArrayIndexOutOfBoundsException e) {}
                try {
                    if (board[coord[6] + coord[7] * width] == 0)
                        bfsQ.add(new MapLocation(coord[6], coord[7]));
                } catch (ArrayIndexOutOfBoundsException e) {}
                try {
                    if (board[coord[0] + coord[1] * width] == 0)
                        bfsQ.add(new MapLocation(coord[0], coord[1]));
                } catch (ArrayIndexOutOfBoundsException e) {}
                break;
            case 2:
                try {
                    if (board[coord[2] + coord[3] * width] == 0)
                        bfsQ.add(new MapLocation(coord[2], coord[3]));
                } catch (ArrayIndexOutOfBoundsException e) {}
                try {
                    if (board[coord[6] + coord[7] * width] == 0)
                        bfsQ.add(new MapLocation(coord[6], coord[7]));
                } catch (ArrayIndexOutOfBoundsException e) {}
                try {
                    if (board[coord[0] + coord[1] * width] == 0)
                        bfsQ.add(new MapLocation(coord[0], coord[1]));
                } catch (ArrayIndexOutOfBoundsException e) {}
                try {
                    if (board[coord[4] + coord[5] * width] == 0)
                        bfsQ.add(new MapLocation(coord[4], coord[5]));
                } catch (ArrayIndexOutOfBoundsException e) {}
                break;
            case 3:
                try {
                    if (board[coord[6] + coord[7] * width] == 0)
                        bfsQ.add(new MapLocation(coord[6], coord[7]));
                } catch (ArrayIndexOutOfBoundsException e) {}
                try {
                    if (board[coord[0] + coord[1] * width] == 0)
                        bfsQ.add(new MapLocation(coord[0], coord[1]));
                } catch (ArrayIndexOutOfBoundsException e) {}
                try {
                    if (board[coord[4] + coord[5] * width] == 0)
                        bfsQ.add(new MapLocation(coord[4], coord[5]));
                } catch (ArrayIndexOutOfBoundsException e) {}
                try {
                    if (board[coord[2] + coord[3] * width] == 0)
                        bfsQ.add(new MapLocation(coord[2], coord[3]));
                } catch (ArrayIndexOutOfBoundsException e) {}
                break;
        }
    }
}
