package dev;

import battlecode.common.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static dev.RobotPlayer.*;

public strictfp class RunCarrier {
    static RobotInfo[] enemyRobots;
    static MapLocation me;
    /**
     * Run a single turn for a Carrier.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
	static void runCarrier(RobotController rc) throws GameActionException {
        updateMap(rc);

        me = rc.getLocation();
        enemyRobots = rc.senseNearbyRobots(-1, rc.getTeam().opponent());

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
		if (rc.getID() % 2 == 0) {
		    pWellLoc = Communication.getClosestWell(rc, ResourceType.ADAMANTIUM);
		} 
		else {
			pWellLoc = Communication.getClosestWell(rc, ResourceType.MANA);
		}
        if (pWellLoc != null) {
            wellLoc = pWellLoc;
        }

        // Try to gather from squares around us.
		boolean foundWell = false;
		if (wellLoc != null && rc.canCollectResource(wellLoc, -1)) {
			rc.collectResource(wellLoc, -1);
            rc.setIndicatorString("Collecting, now have, AD:" +
                    rc.getResourceAmount(ResourceType.ADAMANTIUM) +
                    " MN: " + rc.getResourceAmount(ResourceType.MANA) +
                    " EX: " + rc.getResourceAmount(ResourceType.ELIXIR));
            foundWell = true;
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
                Clock.yield();
            }
            // try to transfer MANA
            if (rc.canTransferResource(HQLOC, ResourceType.MANA, rc.getResourceAmount(ResourceType.MANA))) {
                rc.transferResource(HQLOC, ResourceType.MANA, rc.getResourceAmount(ResourceType.MANA));
                Clock.yield();
            }
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
        List<MapLocation> islandLocs = new ArrayList<>();
        for (int id : islands) {
            MapLocation[] thisIslandLocs = rc.senseNearbyIslandLocations(id);
            islandLocs.addAll(Arrays.asList(thisIslandLocs));
        }
        if (islandLocs.size() > 0) {
            for (int i = 0; i < islandLocs.size(); i++) {
                if (rc.senseAnchor(rc.senseIsland(islandLocs.get(i))) == null) {
                    MapLocation islandLocation = islandLocs.get(i);
                    if (rc.canPlaceAnchor()) {
                        rc.setIndicatorString("Huzzah, placed anchor!");
                        rc.placeAnchor();
                    } else {
                    	rc.setIndicatorString("Moving my anchor towards " + islandLocation);
	    				Direction dir = rc.getLocation().directionTo(islandLocation);
                    	if (rc.canMove(dir)) {
                    	    rc.move(dir);
                    	}
	    			}
                    break;
                }
            }
        }
	    // Also try to move *randomly*.
	    Direction dir = currentDirection;
	    if (rc.canMove(dir)) {
	        rc.move(dir);
	    } else if (rc.getMovementCooldownTurns() == 0) {
	        currentDirection = directions[rng.nextInt(directions.length)];
	    }
    }
}
