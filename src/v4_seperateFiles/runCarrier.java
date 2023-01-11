package v4_seperateFiles;

import battlecode.common.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static v4_seperateFiles.RobotPlayer.*;

public strictfp class runCarrier {
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
                //MapLocation islandLocation = islandLocs.iterator().next();
                for (int i = 0; i < islandLocs.size(); i++) {
                    if (rc.senseAnchor(rc.senseIsland(islandLocs.get(i))) == null) {
                        MapLocation islandLocation = islandLocs.get(i);
                        rc.setIndicatorString("Moving my anchor towards " + islandLocation);
                        Direction dir = rc.getLocation().directionTo(islandLocation);
                        if (rc.canMove(dir)) {
                            rc.move(dir);
                        }
                        if (rc.canPlaceAnchor() && rc.senseAnchor(rc.senseIsland(islandLocation)) == null) {
                            rc.setIndicatorString("Huzzah, placed anchor!");
                            rc.placeAnchor();
                        }
                        break;
                    }
                }
            }
            Direction dir = directions[rng.nextInt(8)];
            rc.setIndicatorString("moving randomly " + dir);
            if (rc.canMove(dir)) {
                rc.move(dir);
            }
            return;
        }
        // Try to gather from squares around us.
        MapLocation me = rc.getLocation();
        boolean foundWell = false;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                MapLocation wellLocation = new MapLocation(me.x + dx, me.y + dy);
                if (rc.canCollectResource(wellLocation, -1)) {
                    rc.collectResource(wellLocation, -1);
                    rc.setIndicatorString("Collecting, now have, AD:" +
                            rc.getResourceAmount(ResourceType.ADAMANTIUM) +
                            " MN: " + rc.getResourceAmount(ResourceType.MANA) +
                            " EX: " + rc.getResourceAmount(ResourceType.ELIXIR));
                    foundWell = true;
                }
            }
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

                if (rc.canTakeAnchor(closest_hq.location, Anchor.STANDARD) && rng.nextInt(20) == 1) {
                    rc.takeAnchor(closest_hq.location, Anchor.STANDARD);
                }

                Direction dir = me.directionTo(closest_hq.location);
                if (rc.canMove(dir)) {
                    rc.move(dir);
                }
            }
        }
        // Occasionally try out the carriers attack
        RobotInfo[] enemyRobots = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        if (enemyRobots.length > 0) {
            if (rc.canAttack(enemyRobots[0].location)) {
                int before = getTotalResources(rc);
                rc.attack(enemyRobots[0].location);
                rc.setIndicatorString("Attacking! before: " + before + " after: " + getTotalResources(rc));
            }
        }

        // If we can see a well, move towards it
        WellInfo[] wells = rc.senseNearbyWells();
        if (wells.length >= 1) {
            Direction dir = me.directionTo(wells[0].getMapLocation());
            if (rc.canMove(dir))
                rc.move(dir);
        }
        if (getTotalResources(rc) == 40) {
            // Also try to move randomly.
            navigateToBugRandom(rc, HQLOC);
        } else {
            if (wellLoc != null) {
                navigateToBugRandom(rc, wellLoc);
            } else {
                // Also try to move *randomly*.
                Direction dir = directions[rng.nextInt(8)];
                rc.setIndicatorString("moving randomly " + dir);
                if (rc.canMove(dir)) {
                    rc.move(dir);
                }
            }
        }
    }
}