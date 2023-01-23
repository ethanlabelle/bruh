package dev;

import battlecode.common.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static dev.RobotPlayer.*;

public strictfp class RunCarrier {
    static RobotInfo[] enemyRobots;
    static MapLocation me;
    static final int BAN_LIST_SIZE = 4;
    static MapLocation[] bannedWells = new MapLocation[BAN_LIST_SIZE];
    static int banCounter = 0;
    static boolean foundWell = false;
    /**
     * Run a single turn for a Carrier.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
	static void runCarrier(RobotController rc) throws GameActionException {
        //System.out.println(""+wellLoc);
        updateMap(rc);
        //System.out.println(""+wellLoc);

        me = rc.getLocation();
        enemyRobots = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        rc.setIndicatorString(""+wellLoc);

		// Try out the carriers attack
        if (enemyRobots.length > 0 && getTotalResources(rc) >= 5) {
            carrierAttack(rc);
        }

        // run away from enemy launchers
		if (enemyRobots.length > 0) {
			Communication.reportEnemy(rc, rc.getLocation());
            runAway(rc);
		}

        if (rc.getAnchor() != null) {
            carryAnchor(rc);
            return;
        }
		
        MapLocation pWellLoc;
		if (rc.getID() % 3 == 0) {
		    pWellLoc = Communication.getClosestWell(rc, ResourceType.ADAMANTIUM);
		} 
		else {
			pWellLoc = Communication.getClosestWell(rc, ResourceType.MANA);
		}
        if (pWellLoc != null && !onBanList(pWellLoc)) {
            wellLoc = pWellLoc;
        }

        foundWell = false;
        // Try to gather from squares around us.
		if (wellLoc != null && rc.canCollectResource(wellLoc, -1)) {
			//rc.collectResource(wellLoc, -1);
            //rc.setIndicatorString("Collecting, now have, AD:" +
            //        rc.getResourceAmount(ResourceType.ADAMANTIUM) +
            //        " MN: " + rc.getResourceAmount(ResourceType.MANA) +
            //        " EX: " + rc.getResourceAmount(ResourceType.ELIXIR));
            //foundWell = true;
            //// if there are too many carriers about this well, forget and ban well
            //if (getTotalResources(rc) == 40) {
            //    RobotInfo[] robotInfos = rc.senseNearbyRobots(me, 4, myTeam);
		    //    RobotInfo[] friends = Arrays.stream(robotInfos).filter(robot -> robot.type == RobotType.CARRIER && robot.team == myTeam).toArray(RobotInfo[]::new);	
            //    rc.setIndicatorString("" + friends.length);
            //    if (friends.length > 6) {
            //        bannedWells[banCounter] = wellLoc;
            //        wellLoc = null;
            //        banCounter = banCounter++ % BAN_LIST_SIZE;
            //    }
            //}
            mine(rc);
		}

        // If at a well, keep collecting until full.
        if (foundWell && getTotalResources(rc) < 40) {
            return;
        }


		// try to deposite resources
        if (getTotalResources(rc) == 40) {
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
        } else {
			// find resources
            if (wellLoc != null) {
                navigateTo(rc, wellLoc);
            } else {
				// Also try to move *randomly*.
				Direction dir = currentDirection;
				if (rc.canMove(dir)) {
				    rc.move(dir);
				} else if (rc.getMovementCooldownTurns() == 0) {
				    currentDirection = directions[rng.nextInt(directions.length)];
				}
				dir = currentDirection;
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
                    return;
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
			        Team team = rc.senseTeamOccupyingIsland(id);
                    MapLocation[] thisIslandLocs = rc.senseNearbyIslandLocations(id);
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
            RobotInfo[] robotInfos = rc.senseNearbyRobots(me, 4, myTeam);
		    RobotInfo[] friends = Arrays.stream(robotInfos).filter(robot -> robot.type == RobotType.CARRIER && robot.team == myTeam).toArray(RobotInfo[]::new);	
            rc.setIndicatorString("" + friends.length);
            if (friends.length > 6) {
                //System.out.println("banning well " + wellLoc);
                bannedWells[banCounter] = wellLoc;
                wellLoc = null;
                banCounter = banCounter++ % BAN_LIST_SIZE;
            }
            //System.out.println(""+wellLoc);
            return;
        }
        for (int i = 0; i < directions.length; i++) {
            MapLocation miningLoc = wellLoc.add(directions[i]);
            if (!miningLoc.equals(me) && board[miningLoc.x][miningLoc.y] != M_STORM && rc.senseMapInfo(miningLoc).getCurrentDirection() == Direction.CENTER && rc.canMove(me.directionTo(miningLoc))) {
                rc.move(me.directionTo(miningLoc));
                return;
            }
        } 
    }
}
