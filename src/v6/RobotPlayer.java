package v6;

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

	// constants for shared array map
	// we have 64 * 16 bits = 2^6 * 2^4 = 2^10 bits = 1024 bits
	
	static final short M_EMPTY = 0b0000;
	static final short M_CLOUD = 0b0001;
	static final short M_STORM = 0b0010;
	static final short M_CURW = 0b0011;
	static final short M_CURN = 0b0100;
	static final short M_CURS = 0b0101;
	static final short M_CURE = 0b0110;
	static final short M_AISL = 0b0111;
	static final short M_BISL = 0b1000;
	static final short M_NISL = 0b1001;
	static final short M_MANA = 0b1010;
	static final short M_ADA = 0b1011;
	static final short M_ELIX = 0b1100;
	static final short M_AHQ = 0b1101;
	static final short M_BHQ = 0b1110;
	static final short M_HIDDEN = 0b1111;

	static short[][] board = new short[60][60];
	static int currentDirectionInd;
	static boolean onObstacle;
	static boolean turnDirection; // true is right, false is left
	static MapLocation HQLOC;
	static MapLocation wellLoc;
	static Team myTeam;

	static void printBoard() {
		String out = "";
		for (int i = 0; i < board.length; i++) {
			for (int j = 0; j < board[i].length; j++) {
				out = out + board[i][j] + " ";
			}
			out = out + "\n";
		}
		System.out.println(out);
	
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
		
		// TODO: clean up initialization
        // Hello world! Standard output is very useful for debugging.
        // Everything you say here will be directly viewable in your terminal when you run a match!
        System.out.println("I'm a " + rc.getType() + " and I just got created! I have health " + rc.getHealth());
		
		if (rc.getType() == RobotType.HEADQUARTERS) {
			setup(rc);
		}

        // You can also use indicators to save debug notes in replays.
        rc.setIndicatorString("Hello world!");
		myTeam = rc.getTeam();
		updateMap(rc);
		if (rc.getType() != RobotType.LAUNCHER) {
			currentDirectionInd = rng.nextInt(directions.length);
		}
		else {
			// default direction for launcher is east
			currentDirectionInd = 2;
		}
		onObstacle = false;
		turnDirection = false;

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
                    case HEADQUARTERS:     RunHeadquarters.runHeadquarters(rc);  break;
                    case CARRIER:      RunCarrier.runCarrier(rc);   break;
                    case LAUNCHER: RunLauncher.runLauncher(rc); break;
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

	// Fills out the static board array for RobotPlayer board 
	// TODO: Approximate bytecode use
	// TODO: Clouds, currents 
	// TODO: HIDDEN tiles on init
	static void updateMap(RobotController rc) throws GameActionException {
		MapInfo[] mapInfos = rc.senseNearbyMapInfos(); // 200 bytecode
		for (MapInfo mapInf : mapInfos) {
			MapLocation loc = mapInf.getMapLocation();
			if(!rc.sensePassability(loc) && board[loc.x][loc.y] == 0b0000) { // <= 180 bytecode (5 * tiles in radius (at most 36))
				board[loc.x][loc.y] = M_STORM;
				//System.out.println("found wall " + loc.x + ", " + loc.y);
			}
		}

        RobotInfo[] hqs = Arrays.stream(rc.senseNearbyRobots()).filter(robot -> robot.type == RobotType.HEADQUARTERS).toArray(RobotInfo[]::new);
		for (RobotInfo hq : hqs) {
			MapLocation loc = hq.getLocation();
			Team team = hq.getTeam();
			if (board[loc.x][loc.y] == 0b0000) {
				if (team == Team.A) {
					//System.out.println("found Team A HQ " + loc.x + ", " + loc.y);
					board[loc.x][loc.y] = M_AHQ;	
				} else {
					//System.out.println("found Team B HQ " + loc.x + ", " + loc.y);
					board[loc.x][loc.y] = M_BHQ;	
				}
			}
			if (myTeam == team)
				HQLOC = loc;
		}
		
		WellInfo[] wells = rc.senseNearbyWells(); // 100 bytecode
		for (WellInfo wellInfo : wells) {
			MapLocation loc = wellInfo.getMapLocation();
			switch (wellInfo.getResourceType()) {
				case MANA:
					//System.out.println("found MANA well " + loc.x + ", " + loc.y);
					board[loc.x][loc.y] = M_MANA;
					break;
				case ADAMANTIUM:
					//System.out.println("found ADA well " + loc.x + ", " + loc.y);
					board[loc.x][loc.y] = M_ADA;
					break;
				case ELIXIR:	
					//System.out.println("found ELIXIR well " + loc.x + ", " + loc.y);
					board[loc.x][loc.y] = M_ELIX;
					break;
			}
			// track mana wells
			if (rc.getID() % 2 == 0 && wellInfo.getResourceType() == ResourceType.MANA) {
				wellLoc = loc;
			} 
			if (rc.getID() % 2 == 1 && wellInfo.getResourceType() == ResourceType.ADAMANTIUM) {
				wellLoc = loc;
			}
		}
		int[] islands = rc.senseNearbyIslands(); // 200 bytecode
		for (int id : islands) {
            MapLocation[] islandLocs = rc.senseNearbyIslandLocations(id);
			Team team = rc.senseTeamOccupyingIsland(id);
			for (MapLocation loc : islandLocs) {
				if (team == Team.A) {
					//System.out.println("found Team A island " + loc.x + ", " + loc.y);
					board[loc.x][loc.y] = M_AISL;
				} else if (team == Team.B) {
					//System.out.println("found Team B island " + loc.x + ", " + loc.y);
					board[loc.x][loc.y] = M_BISL;
				} else {
					//System.out.println("found Neutral island " + loc.x + ", " + loc.y);
					board[loc.x][loc.y] = M_NISL;
				}
			}
		}
	}

	static void navigateToBugRandom(RobotController rc, MapLocation loc) throws GameActionException {
		// head towards goal
        rc.setIndicatorString("Navigating to " + loc);
        Direction goalDir = rc.getLocation().directionTo(loc);
        if (rc.canMove(goalDir)) {
            rc.move(goalDir);
			return;
        } else { // indicates obstacle
			// move random
			Direction dir = directions[rng.nextInt(8)];
        	if (rc.canMove(dir)) {
        	    rc.move(dir);
        	}
		}
	}
	
	static void turnRight() throws GameActionException {
		currentDirectionInd++;
		currentDirectionInd %= directions.length;
	}	

	static void turnLeft() throws GameActionException {
		currentDirectionInd--;
		if (currentDirectionInd < 0)
			currentDirectionInd += directions.length;
	}	

	static int directionToIndex(Direction dir) throws GameActionException {
		for (int i = 0; i < directions.length; i++) {
			if (directions[i] == dir) {
				return i;
			}
		}
		// should never happen
		return -1;
	}

	static boolean senseRight(RobotController rc) throws GameActionException {
		int senseDir = (currentDirectionInd + 1) % directions.length;
		MapLocation tile = rc.getLocation().add(directions[senseDir]);
		if (!rc.onTheMap(tile))
			return true;
		return board[tile.x][tile.y] == M_STORM || board[tile.x][tile.y] == M_AHQ || board[tile.x][tile.y] == M_BHQ;
	}

	static boolean senseFront(RobotController rc) throws GameActionException {
		MapLocation tile = rc.getLocation().add(directions[currentDirectionInd]);
		if (!rc.onTheMap(tile))
			return true;
		return board[tile.x][tile.y] == M_STORM || board[tile.x][tile.y] == M_AHQ || board[tile.x][tile.y] == M_BHQ;
	}

	static void navigateToBug0(RobotController rc, MapLocation loc) throws GameActionException {
		// head towards goal
        rc.setIndicatorString("Navigating to " + loc);
        Direction goalDir = rc.getLocation().directionTo(loc);
        if (rc.canMove(goalDir)) {
			onObstacle = false;
			currentDirectionInd = directionToIndex(goalDir);
            rc.move(goalDir);
			return;
        } else {
			if (!onObstacle) {
				MapLocation pathTile = rc.getLocation().add(goalDir);
				if (board[pathTile.x][pathTile.y] == M_STORM || rng.nextInt(4) == 1) { // indicates obstacle
					onObstacle = true;
					currentDirectionInd = directionToIndex(goalDir);
					turnLeft();
					goalDir = directions[currentDirectionInd];
					if (rc.canMove(goalDir)) {
						rc.move(goalDir);
					}
				}
			} else {
				rc.setIndicatorString("on obstacle");
				// follow obstacle using right hand rule
				boolean right = senseRight(rc);
				if (!right) {
					turnRight();
					goalDir = directions[currentDirectionInd];
					if (rc.canMove(goalDir)) {
						rc.move(goalDir);
					}
				}
				boolean front = senseFront(rc);
				if (!front) {
					goalDir = directions[currentDirectionInd];
					if (rc.canMove(goalDir)) {
						rc.move(goalDir);
					}
				} else {
					turnLeft();
				}

			}
		}
	}

	/*
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

		Can return null if there is not a free tile nearby
		TODO: Check all tiles in action radius
				i.e. sense MapInfos to radius & do set difference with occupied squares
	 */
	static MapLocation getSpawnLocation(RobotController rc, RobotType unit) throws GameActionException {
        // Pick a direction to build in.
        for (Direction checkDir : directions) {
            MapLocation newLoc = rc.getLocation().add(checkDir);
			if (rc.canBuildRobot(unit, newLoc)) 
				return newLoc;
        }
		return null;
	}

	static void setup(RobotController rc) throws GameActionException {
		int i = 0;
		while (i < 4) {
            rc.setIndicatorString("Trying to build a carrier");
			MapLocation loc = getSpawnLocation(rc, RobotType.CARRIER);
            if (loc != null) {
                rc.buildRobot(RobotType.CARRIER, loc);
				i++;
            }
			Clock.yield();
		}	
		i = 0;
		while (i < 4) {
            rc.setIndicatorString("Trying to build a launcher");
			MapLocation loc = getSpawnLocation(rc, RobotType.LAUNCHER);
            if (loc != null) {
                rc.buildRobot(RobotType.LAUNCHER, loc);
				i++;
            }
			Clock.yield();
		}	
	}
	
	static int getTotalResources(RobotController rc) throws GameActionException {
		return rc.getResourceAmount(ResourceType.ADAMANTIUM) + rc.getResourceAmount(ResourceType.MANA) + rc.getResourceAmount(ResourceType.ELIXIR);
	}
}
