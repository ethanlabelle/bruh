package v3_1;

import battlecode.common.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;

/**
 * RobotPlayer is the class that describes your main robot strategy.
 * The run() method inside this class is like your main function: this is what we'll call once your robot
 * is created!
 */
public strictfp class RobotPlayer {

    /**
     * We will use this variable to count the number of turns this robot has been alive.
     * You can use static variables like this to save any information you want. Keep in mind that even though
     * these variables are static, in Battlecode they aren't actually shared between your robots.
     */
    static int turnCount = 0;

    /**
     * A random number generator.
     * We will use this RNG to make some random moves. The Random class is provided by the java.util.Random
     * import at the top of this file. Here, we *seed* the RNG with a constant number (6147); this makes sure
     * we get the same sequence of numbers every time this code is run. This is very useful for debugging!
     */
    static final Random rng = new Random(6147);

    /** Array containing all the possible movement directions. */
    static final Direction[] directions = {
        Direction.NORTH,
        Direction.NORTHEAST,
        Direction.EAST,
        Direction.SOUTHEAST,
        Direction.SOUTH,
        Direction.SOUTHWEST,
        Direction.WEST,
        Direction.NORTHWEST,
    };

    static Direction oppositeDirection (Direction dir) {
        if (dir == Direction.NORTH) {
            return Direction.SOUTH;
        }
        if (dir == Direction.NORTHEAST) {
            return Direction.SOUTHWEST;
        }
        if (dir == Direction.EAST) {
            return Direction.WEST;
        }
        if (dir == Direction.SOUTHEAST) {
            return Direction.NORTHWEST;
        }
        if (dir == Direction.SOUTH) {
            return Direction.NORTH;
        }
        if (dir == Direction.SOUTHWEST) {
            return Direction.NORTHEAST;
        }
        if (dir == Direction.WEST) {
            return Direction.EAST;
        }
        return Direction.SOUTHEAST;
    }



    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * It is like the main function for your robot. If this method returns, the robot dies!
     *
     * @param rc  The RobotController object. You use it to perform actions from this robot, and to get
     *            information on its current status. Essentially your portal to interacting with the world.
     **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {

        // Hello world! Standard output is very useful for debugging.
        // Everything you say here will be directly viewable in your terminal when you run a match!
        System.out.println("I'm a " + rc.getType() + " and I just got created! I have health " + rc.getHealth());

        // You can also use indicators to save debug notes in replays.
        rc.setIndicatorString("Hello world!");

        while (true) {
            // This code runs during the entire lifespan of the robot, which is why it is in an infinite
            // loop. If we ever leave this loop and return from run(), the robot dies! At the end of the
            // loop, we call Clock.yield(), signifying that we've done everything we want to do.

            turnCount += 1;  // We have now been alive for one more turn!

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode.
            try {
                // The same run() function is called for every robot on your team, even if they are
                // different types. Here, we separate the control depending on the RobotType, so we can
                // use different strategies on different robots. If you wish, you are free to rewrite
                // this into a different control structure!
                switch (rc.getType()) {
                    case HEADQUARTERS:     runHeadquarters(rc);  break;
                    case CARRIER:      runCarrier(rc);   break;
                    case LAUNCHER: runLauncher(rc); break;
                    case BOOSTER: // Examplefuncsplayer doesn't use any of these robot types below.
                    case DESTABILIZER: // You might want to give them a try!
                    case AMPLIFIER:       break;
                }

            } catch (GameActionException e) {
                // Oh no! It looks like we did something illegal in the Battlecode world. You should
                // handle GameActionExceptions judiciously, in case unexpected events occur in the game
                // world. Remember, uncaught exceptions cause your robot to explode!
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();

            } catch (Exception e) {
                // Oh no! It looks like our code tried to do something bad. This isn't a
                // GameActionException, so it's more likely to be a bug in our code.
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();

            } finally {
                // Signify we've done everything we want to do, thereby ending our turn.
                // This will make our code wait until the next turn, and then perform this loop again.
                Clock.yield();
            }
            // End of loop: go back to the top. Clock.yield() has ended, so it's time for another turn!
        }

        // Your code should never reach here (unless it's intentional)! Self-destruction imminent...
    }

    /**
     * Run a single turn for a Headquarters.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
    static void runHeadquarters(RobotController rc) throws GameActionException {
        // Pick a direction to build in.
        boolean found = false;
        MapLocation newLoc = null;
        for (Direction checkDir : directions) {
            newLoc = rc.getLocation().add(checkDir);
            if (!rc.canSenseRobotAtLocation(newLoc)) {
                found = true;
                break;
            }
        }
        //Direction dir = directions[rng.nextInt(directions.length)];
        //MapLocation newLoc = rc.getLocation().add(dir);
        if (!found) {
            return;
        }
        if (rc.canBuildAnchor(Anchor.STANDARD) && rng.nextInt(10) == 1) {
            // If we can build an anchor do it!
            rc.buildAnchor(Anchor.STANDARD);
            rc.setIndicatorString("Building anchor! " + rc.getAnchor());
        } 
		if (rng.nextBoolean()) {
            // Let's try to build a carrier.
            rc.setIndicatorString("Trying to build a carrier");
            if (rc.canBuildRobot(RobotType.CARRIER, newLoc)) {
                rc.buildRobot(RobotType.CARRIER, newLoc);
            }
        } else {
            // Let's try to build a launcher.
            rc.setIndicatorString("Trying to build a launcher");
            if (rc.canBuildRobot(RobotType.LAUNCHER, newLoc)) {
                rc.buildRobot(RobotType.LAUNCHER, newLoc);
            }
        }
    }
	
	static int getTotalResources(RobotController rc) throws GameActionException {
		return rc.getResourceAmount(ResourceType.ADAMANTIUM) + rc.getResourceAmount(ResourceType.MANA) + rc.getResourceAmount(ResourceType.ELIXIR);
	}

    /**
     * Run a single turn for a Carrier.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
    static void runCarrier(RobotController rc) throws GameActionException {
        Team opponent = rc.getTeam().opponent();
        if (rc.getAnchor() != null) {
            // If I have an anchor singularly focus on getting it to the first island I see
            int[] islands = rc.senseNearbyIslands();
            //MapLocation[] islands = Arrays.stream(rc.senseNearbyIslands()).filter(
			//		robot -> robot.type == RobotType.HEADQUARTERS && robot.team != opponent).toArray(RobotInfo[]::new);
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
				//while (islandLocs.iterator().hasNext() && rc.senseAnchor(rc.senseIsland(islandLocation)) != null) {
				//	islandLocation = islandLocs.iterator().next();
				//	System.out.println(islandLocs);
				//	System.out.println(islandLocation);
				//}
				
				// TODO: ignore islands controlled by player		
                //rc.setIndicatorString("Moving my anchor towards " + islandLocation);
                //while (!rc.getLocation().equals(islandLocation)) {
                    //Direction dir = rc.getLocation().directionTo(islandLocation);
                    //if (rc.canMove(dir)) {
                    //    rc.move(dir);
                    //}
                //}
                //if (rc.canPlaceAnchor() && rc.senseAnchor(rc.senseIsland(islandLocation)) == null) {
                //    rc.setIndicatorString("Huzzah, placed anchor!");
                //    rc.placeAnchor();
                //}
            }
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
        if (rng.nextInt(20) == 1) {
            RobotInfo[] enemyRobots = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
            if (enemyRobots.length > 0) {
                if (rc.canAttack(enemyRobots[0].location)) {
					int before = getTotalResources(rc);
                    rc.attack(enemyRobots[0].location);
					rc.setIndicatorString("Attacking! before: " + before + " after: " + getTotalResources(rc));
                }
            }
        }
       	
        // If we can see a well, move towards it
        WellInfo[] wells = rc.senseNearbyWells();
        if (wells.length >= 1 && rng.nextInt(3) == 1) {
            Direction dir = me.directionTo(wells[0].getMapLocation());
            if (rc.canMove(dir)) 
                rc.move(dir);
        }
        // Also try to move randomly.
        Direction dir = directions[rng.nextInt(directions.length)];
        if (rc.canMove(dir)) {
            rc.move(dir);
        }
    }

    /**
     * Run a single turn for a Launcher.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
    static int pastHealth = 0;
    static void runLauncher(RobotController rc) throws GameActionException {
        // this initializes the health
        if (pastHealth == 0) {
            pastHealth = rc.getHealth();
        }
        // Try to attack someone
        int radius = rc.getType().actionRadiusSquared;
        Team opponent = rc.getTeam().opponent();
        RobotInfo[] enemies = rc.senseNearbyRobots(radius, opponent);
        if (enemies.length > 0) {
            MapLocation toAttack = enemies[0].location;  // make this target enemy firings first
            //MapLocation toAttack = rc.getLocation().add(Direction.EAST);

            if (rc.canAttack(toAttack)) {
                rc.setIndicatorString("Attacking");        
                rc.attack(toAttack);
            }
        }

        //TODO: ATTACK ENEMY HEADQUARTERS!!!!!!!!!!!!!!!!

        // if taking damage retreat, prob get rid of here and add to the non attack robots but implement better than one turn.
        int currentHealth = rc.getHealth();
        if (currentHealth < pastHealth && enemies.length > 0) {
            Direction badDir = rc.getLocation().directionTo(enemies[0].location);
            Direction dir = oppositeDirection(badDir);
            if (rc.canMove(dir)) {
                rc.move(dir);
                return;
            }
        }

        // Also try to move randomly.
        Direction dir = directions[rng.nextInt(directions.length)];
        if (rc.canMove(dir)) {
            rc.move(dir);
        }
    }
}
