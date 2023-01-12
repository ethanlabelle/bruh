package dev;

import battlecode.common.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static dev.RobotPlayer.*;

public strictfp class RunCarrier {
    /**
     * Run a single turn for a Carrier.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
    static void runCarrier(RobotController rc) throws GameActionException {
        Team opponent = rc.getTeam().opponent();
        updateMap(rc);

        if (rc.getAnchor() != null) {
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
			Direction dir = directions[currentDirectionInd];
			if (rc.canMove(dir)) {
			    rc.move(dir);
			} else if (rc.getMovementCooldownTurns() == 0) {
			    currentDirectionInd = rng.nextInt(directions.length);
			}
            return;
        }

        // Try out the carriers attack
        RobotInfo[] enemyRobots = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        if (enemyRobots.length > 0 && getTotalResources(rc) >= 5) {
            if (rc.canAttack(enemyRobots[0].location)) {
                int before = getTotalResources(rc);
                rc.attack(enemyRobots[0].location);
                rc.setIndicatorString("Attacking! before: " + before + " after: " + getTotalResources(rc));
            }
        }

        // Try to gather from squares around us.
        MapLocation me = rc.getLocation();
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

        if (getTotalResources(rc) == 40) {
            rc.setIndicatorString("Full resources!");
            RobotInfo[] hqs = Arrays.stream(rc.senseNearbyRobots()).filter(robot -> robot.type == RobotType.HEADQUARTERS && robot.team != opponent).toArray(RobotInfo[]::new);
            int min_dist = 7200;
            if (hqs.length >= 1) {
                RobotInfo closest_hq = hqs[0];
                for (RobotInfo hq: hqs) {
                    int dist = hq.location.distanceSquaredTo(me);
                    if (dist < min_dist) {
                        min_dist = dist;
                        closest_hq = hq;
                    }
                }
                // try to transfer ADAMANTIUM
                if (rc.canTransferResource(closest_hq.location, ResourceType.ADAMANTIUM, rc.getResourceAmount(ResourceType.ADAMANTIUM))) {
                    rc.transferResource(closest_hq.location, ResourceType.ADAMANTIUM, rc.getResourceAmount(ResourceType.ADAMANTIUM));
                    Clock.yield();
                }

                // try to transfer MANA
                if (rc.canTransferResource(closest_hq.location, ResourceType.MANA, rc.getResourceAmount(ResourceType.MANA))) {
                    rc.transferResource(closest_hq.location, ResourceType.MANA, rc.getResourceAmount(ResourceType.MANA));
                    Clock.yield();
                }

                if (rc.canTakeAnchor(closest_hq.location, Anchor.STANDARD) && rng.nextInt(10) == 1) {
                    rc.takeAnchor(closest_hq.location, Anchor.STANDARD);
                }

                Direction dir = me.directionTo(closest_hq.location);
                if (rc.canMove(dir)) {
                    rc.move(dir);
                }
            }
        }

        if (getTotalResources(rc) == 40) {
            navigateTo(rc, HQLOC);
        } else {
            if (wellLoc != null) {
                navigateTo(rc, wellLoc);
            } else {
				// Also try to move *randomly*.
				Direction dir = directions[currentDirectionInd];
				if (rc.canMove(dir)) {
				    rc.move(dir);
				} else if (rc.getMovementCooldownTurns() == 0) {
				    currentDirectionInd = rng.nextInt(directions.length);
				}
            }
        }
    }
}
