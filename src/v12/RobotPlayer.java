package v12;

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
     * A random number generator.
     * We will use this RNG to make some random moves. The Random class is provided by the java.util.Random
     * import at the top of this file. Here, we *seed* the RNG with a constant number (6147); this makes sure
     * we get the same sequence of numbers every time this code is run. This is very useful for debugging!
     */
    static final Random rng = new Random(6147);
    //static final Random rng = new Random();

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

	static final int MAX_FRIENDS = 5;

	// constants for local map
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

	// pathfinding state
	static int currentDirectionInd;
	static boolean onObstacle;
	static int maxDistSquared = 7200;
	static int checkPointSquared = maxDistSquared;
	static int width;
	static int height;

	// map state
	static MapLocation HQLOC;
	static MapLocation EnemyHQLOC;
	static MapLocation spawnHQLOC;
	static MapLocation wellLoc;
	static MapInfo[] mapInfos; // rc.senseNearbyMapInfos();
	static RobotInfo[] robotInfos; // rc.senseNearbyRobots();
	static int[] islands; // rc.senseNearbyIslands();
	static WellInfo[] nearbyWells; // rc.senseNearbyWells();
	
	// general robot state
    static int turnCount = 0; // number of turns robot has been alive
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

		width = rc.getMapWidth();
		height = rc.getMapHeight();

        rc.setIndicatorString("Hello world!");
		myTeam = rc.getTeam();
		updateMap(rc);
		if (rc.getType() != RobotType.LAUNCHER) {
			currentDirectionInd = rng.nextInt(directions.length);
		}
		else {
			// make launcher go the opposite direction of the quadrant they were spawned in
			MapLocation loc = rc.getLocation();
			int x = loc.x;
			int y = loc.y;
			int map_width = rc.getMapWidth();
			int map_height = rc.getMapHeight();
			if (x < map_width / 2 && y < map_height / 2) {
				currentDirectionInd = 1;
			}
			else if (x < map_width / 2 && y >= map_height / 2) {
				currentDirectionInd = 3;
			}
			else if (x >= map_width / 2 && y < map_height / 2) {
				currentDirectionInd = 7;
			}
			else {
				currentDirectionInd = 5;
			}
		}
		onObstacle = false;
		
		if (rc.getType() == RobotType.HEADQUARTERS) {
			setup(rc);
		}


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
                    case AMPLIFIER:       RunAmplifier.runAmplifier(rc); break;
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
		mapInfos = rc.senseNearbyMapInfos(); // 200 bytecode
		robotInfos = rc.senseNearbyRobots();
		islands = rc.senseNearbyIslands(); // 200 bytecode
		nearbyWells = rc.senseNearbyWells(); // 100 bytecode
		Communication.tryWriteMessages(rc);

		for (MapInfo mapInf : mapInfos) {
			MapLocation loc = mapInf.getMapLocation();
			if((!rc.sensePassability(loc) || mapInf.getCurrentDirection() != Direction.CENTER) && board[loc.x][loc.y] == 0b0000) { // <= 180 bytecode (5 * tiles in radius (at most 36))
				board[loc.x][loc.y] = M_STORM;
			}
		}

		// search for HQs
		// save location of HQ
        RobotInfo[] hqs = Arrays.stream(robotInfos).filter(robot -> robot.type == RobotType.HEADQUARTERS).toArray(RobotInfo[]::new);
		for (RobotInfo hq : hqs) {
			MapLocation loc = hq.getLocation();
			Team team = hq.getTeam();
			if (board[loc.x][loc.y] == 0b0000) {
				if (team == Team.A) {
					board[loc.x][loc.y] = M_AHQ;	
				} else {
					board[loc.x][loc.y] = M_BHQ;	
				}
			}
			if (myTeam == team) {
				HQLOC = loc;
				if (turnCount == 0)
					spawnHQLOC = loc;
			}
			else if(EnemyHQLOC == null){
				EnemyHQLOC = loc;
			}
		}

		RobotInfo[] friends = Arrays.stream(robotInfos).filter(robot -> robot.type == RobotType.CARRIER && robot.team == myTeam).toArray(RobotInfo[]::new);	
		for (WellInfo wellInfo : nearbyWells) {
			MapLocation loc = wellInfo.getMapLocation();
			switch (wellInfo.getResourceType()) {
				case MANA:
					board[loc.x][loc.y] = M_MANA;
					break;
				case ADAMANTIUM:
					board[loc.x][loc.y] = M_ADA;
					break;
				case ELIXIR:	
					board[loc.x][loc.y] = M_ELIX;
					break;
			}
			if (wellLoc == null || friends.length > MAX_FRIENDS) {
				// Odd ID get ADA, even get MANA
				if (rc.getID() % 2 == 0 && wellInfo.getResourceType() == ResourceType.ADAMANTIUM) {
					wellLoc = loc;
				} 
				else if (rc.getID() % 2 == 1 && wellInfo.getResourceType() == ResourceType.MANA) {
					wellLoc = loc;
				}
			}
			MapLocation arrayLoc;
			switch (wellInfo.getResourceType()) {
				case MANA:
					arrayLoc = Communication.readManaWellLocation(rc);
					if (arrayLoc == null) {
						Communication.updateManaWellLocation(rc, loc);
					}
					break;
				case ADAMANTIUM:
					arrayLoc = Communication.readAdaWellLocation(rc);
					if (arrayLoc == null) {
						Communication.updateAdaWellLocation(rc, loc);
					}
					break;
				default:
					break;
			}
		}

		for (int id : islands) {
            MapLocation[] islandLocs = rc.senseNearbyIslandLocations(id);
			Team team = rc.senseTeamOccupyingIsland(id);
			for (MapLocation loc : islandLocs) {
				if (team == Team.A) {
					board[loc.x][loc.y] = M_AISL;
				} else if (team == Team.B) {
					board[loc.x][loc.y] = M_BISL;
				} else {
					board[loc.x][loc.y] = M_NISL;
				}
			}
		}
	}

	// Navigation
	static void navigateTo(RobotController rc, MapLocation loc) throws GameActionException {
		bug0(rc, loc);
		if (rc.getType() == RobotType.CARRIER)
			bug0(rc, loc);
	}
	
	static boolean tryMove(RobotController rc, Direction dir) throws GameActionException {
        if (rc.canMove(dir)) {
            rc.move(dir);
			return true;
        } else {
			int dirInd = directionToIndex(dir);
			int right = (dirInd + 1) % 8;
			int left = dirInd - 1;
			if (left < 0)
				left += 8;
			if (rc.canMove(directions[right])) {
				rc.move(directions[right]);
				return true;
			}
			if (rc.canMove(directions[left])) {
				rc.move(directions[left]);
				return true;
			}
		}
		return false;	
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

	static Direction oppositeDirection (Direction dir) {
		switch (dir) {
			case NORTH:
				return Direction.SOUTH;
			case NORTHEAST:
				return Direction.SOUTHWEST;
			case SOUTHEAST:
				return Direction.NORTHWEST;
			case SOUTH:
				return Direction.NORTH;
			case EAST:
				return Direction.WEST;
			case WEST:
				return Direction.EAST;
			case SOUTHWEST:
				return Direction.NORTHEAST;
			default:
				return Direction.SOUTHEAST;
		}
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

	static void bugRandom(RobotController rc, MapLocation loc) throws GameActionException {
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

	static void bug0(RobotController rc, MapLocation loc) throws GameActionException {
		// head towards goal
        rc.setIndicatorString("Navigating to " + loc);
        Direction goalDir = rc.getLocation().directionTo(loc);
        if (rc.canMove(goalDir)) {
			rc.move(goalDir);
			onObstacle = false;
			currentDirectionInd = directionToIndex(goalDir);
			return;
        } else {
			if (!onObstacle) {
				MapLocation pathTile = rc.getLocation().add(goalDir);
				if (board[pathTile.x][pathTile.y] == M_STORM || rng.nextInt(3) == 1) { // indicates obstacle
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
					tryMove(rc, goalDir);
				}
				boolean front = senseFront(rc);
				if (!front) {
					goalDir = directions[currentDirectionInd];
					tryMove(rc, goalDir);
				} else {
					turnLeft();
				}

			}
		}
	}
	static void bug2(RobotController rc, MapLocation loc) throws GameActionException {
		// head towards goal
		rc.setIndicatorString("Navigating to " + loc);
		Direction goalDir = rc.getLocation().directionTo(loc);
		if (rc.canMove(goalDir) && rc.getLocation().distanceSquaredTo(loc) < checkPointSquared) {
			rc.move(goalDir);
			onObstacle = false;
			checkPointSquared = maxDistSquared;
			currentDirectionInd = directionToIndex(goalDir);
		} else {
			if (!onObstacle) {
				MapLocation pathTile = rc.getLocation().add(goalDir);
				if (board[pathTile.x][pathTile.y] == M_STORM || rng.nextInt(3) == 1) { // indicates obstacle
					onObstacle = true;
					checkPointSquared = rc.getLocation().distanceSquaredTo(loc);
					currentDirectionInd = directionToIndex(goalDir);
					turnLeft();
					goalDir = directions[currentDirectionInd];
					if (rc.canMove(goalDir)) {
						rc.move(goalDir);
					}
				}
			} else {
				//TODO this doesn't include doing a 180
				rc.setIndicatorString("on obstacle");
				// follow obstacle using right hand rule
				boolean right = senseRight(rc);
				if (!right) {
					turnRight();
					goalDir = directions[currentDirectionInd];
					tryMove(rc, goalDir);
				}
				boolean front = senseFront(rc);
				if (!front) {
					goalDir = directions[currentDirectionInd];
					tryMove(rc, goalDir);
				} else {
					turnLeft();
				}

			}
		}
	}

	static MapLocation getClosestLocation (RobotController rc, MapLocation loc, RobotType unit) throws GameActionException {
		// this is the possible locations it can be the closest to
		MapLocation[] possLoc = rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), RobotType.HEADQUARTERS.actionRadiusSquared);
		int minDist = 7200;
		MapLocation bestLoc = null;
		for (MapLocation checkLoc : possLoc) {
			// rc.canSenseRobotAtLocation(MapLocation loc) always returned false, spawned robot on top of robot and deleted headquarters
			if (rc.canBuildRobot(unit, checkLoc)) {
				int checkDist = checkLoc.distanceSquaredTo(loc);
				if (checkDist < minDist) {
					bestLoc = checkLoc;
					minDist = checkDist;
				}
			}
		}
		return bestLoc;
	}

	static MapLocation getSpawnLocation(RobotController rc, RobotType unit) throws GameActionException {
		// TODO: add target well
		WellInfo [] wells = rc.senseNearbyWells();
		if (unit == RobotType.CARRIER) {
			if (wells.length > 0) {
				MapLocation closeWell = getClosestLocation(rc, wells[0].getMapLocation(), unit);
				if (closeWell != null) {
					return closeWell;
				}
			}
		} else if (unit == RobotType.LAUNCHER) {
            MapLocation center = new MapLocation(width/2, height/2);
			MapLocation spawnLoc = getClosestLocation(rc, center, unit);
			if (spawnLoc != null) {
				return spawnLoc;
			}
		}
		
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
		while (i < 3) {
            rc.setIndicatorString("Trying to build a launcher");
			MapLocation loc = getSpawnLocation(rc, RobotType.LAUNCHER);
            if (loc != null) {
                rc.buildRobot(RobotType.LAUNCHER, loc);
				i++;
            }
			Clock.yield();
		}	
		i = 0;
		while (i < 4) {
            rc.setIndicatorString("Trying to build a carrier");
			MapLocation loc = getSpawnLocation(rc, RobotType.CARRIER);
            if (loc != null) {
                rc.buildRobot(RobotType.CARRIER, loc);
				i++;
            }
			Clock.yield();
		}	
	}
	
	static int getTotalResources(RobotController rc) throws GameActionException {
		return rc.getResourceAmount(ResourceType.ADAMANTIUM) + rc.getResourceAmount(ResourceType.MANA) + rc.getResourceAmount(ResourceType.ELIXIR);
	}

	static int abs(int x) {
		return x < 0 ? -x : x;
	}
}
