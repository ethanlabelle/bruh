package v17;

import battlecode.common.*;

import static v17.Communication.*;

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

	// constants for local map
	// we have 64 * 16 bits = 2^6 * 2^4 = 2^10 bits = 1024 bits
	static final short M_HIDDEN = 0b0000;
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
	static final short M_EMPTY = 0b1111;
	static byte[][] board;

	// pathfinding state
	static Direction currentDirection;
	static boolean onObstacle;
	static int maxDistSquared = 7200;
	static int checkPointSquared = maxDistSquared;
	static int width;
	static int height;

	// map state
	static MapLocation HQLOC;
	static MapLocation EnemyHQLOC;
	static MapLocation wellLoc;
	static MapInfo[] mapInfos; // rc.senseNearbyMapInfos();
	static RobotInfo[] robotInfos; // rc.senseNearbyRobots();
	static int[] islands; // rc.senseNearbyIslands();
	static WellInfo[] nearbyWells; // rc.senseNearbyWells();
	
	// general robot state
    static int turnCount = 0; // number of turns robot has been alive
	static int navCount = 0;
	static Team myTeam;
	static Team enemyTeam;
	
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
		board = new byte[width][height];

        rc.setIndicatorString("Hello world!");
		myTeam = rc.getTeam();
		enemyTeam = myTeam.opponent();
		if (rc.getType() != RobotType.LAUNCHER) {
			currentDirection = directions[rng.nextInt(directions.length)];
		}
		else {
			// make launcher go the opposite direction of the quadrant they were spawned in
			MapLocation loc = rc.getLocation();
			int x = loc.x;
			int y = loc.y;
			int map_width = rc.getMapWidth();
			int map_height = rc.getMapHeight();
			if (x < map_width / 2 && y < map_height / 2) {
				currentDirection = Direction.NORTHEAST;
			}
			else if (x < map_width / 2 && y >= map_height / 2) {
				currentDirection = Direction.SOUTHEAST;
			}
			else if (x >= map_width / 2 && y < map_height / 2) {
				currentDirection = Direction.NORTHWEST;
			}
			else {
				currentDirection = Direction.SOUTHWEST;
			}
		}
		onObstacle = false;
		
		if (rc.getType() == RobotType.HEADQUARTERS) {
			Communication.addHeadquarter(rc);
			Communication.tryWriteMessages(rc);
			HQLOC = rc.getLocation();
			updateMap(rc);
			setup(rc);
			Communication.updateHeadquarterInfo(rc);
		} else {
			Communication.updateHeadquarterInfo(rc);
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
	// About 3000 bytecode or so 
	// TODO: Clouds, currents 
	// TODO: HIDDEN tiles on init
	static void updateMap(RobotController rc) throws GameActionException {
		mapInfos = rc.senseNearbyMapInfos(); // 200 bytecode
		robotInfos = rc.senseNearbyRobots();
		islands = rc.senseNearbyIslands(); // 200 bytecode
		nearbyWells = rc.senseNearbyWells(); // 100 bytecode

        int length = mapInfos.length;
        for (int i = length; --i >= 0;) {
            MapInfo mapInf = mapInfos[i];
			MapLocation loc = mapInf.getMapLocation();
			if (board[loc.x][loc.y] == M_HIDDEN) {
				if (!rc.sensePassability(loc))
					board[loc.x][loc.y] = M_STORM;
				else
					board[loc.x][loc.y] = M_EMPTY;
			}
		}

        length = robotInfos.length;
        for (int i = length; --i >= 0;) {
            RobotInfo robot = robotInfos[i];
            if (robot.getType() == RobotType.HEADQUARTERS) {
			    MapLocation loc = robot.getLocation();
			    Team team = robot.getTeam();
			    if (team == Team.A) {
			    	board[loc.x][loc.y] = M_AHQ;	
			    } else {
			    	board[loc.x][loc.y] = M_BHQ;	
			    }
			    if (myTeam == team) {
			    	HQLOC = loc;
			    }
			    if (myTeam != team && EnemyHQLOC == null){
			    	EnemyHQLOC = loc;
			    }
            }
        }

        length = nearbyWells.length;
		for (int i = length; --i >= 0;) {
            WellInfo wellInfo = nearbyWells[i];
			MapLocation arrayLoc;
			MapLocation loc = wellInfo.getMapLocation();
			switch (wellInfo.getResourceType()) {
				case MANA:
					board[loc.x][loc.y] = M_MANA;
					arrayLoc = Communication.readManaWellLocation(rc, HQLOC);
                    if (arrayLoc == null || loc.distanceSquaredTo(HQLOC) < arrayLoc.distanceSquaredTo(HQLOC))
				        Communication.updateManaWellLocation(rc, loc, HQLOC);
					if (wellLoc == null || loc.distanceSquaredTo(HQLOC) < wellLoc.distanceSquaredTo(HQLOC)) {
						if (rc.getID() % RunCarrier.CARRIER_DIFF_MOD != 0 && !RunCarrier.onBanList(loc))
							wellLoc = loc;
					}
					break;
				case ADAMANTIUM:
					arrayLoc = Communication.readAdaWellLocation(rc, HQLOC);
                    if (arrayLoc == null || loc.distanceSquaredTo(HQLOC) < arrayLoc.distanceSquaredTo(HQLOC))
					    Communication.updateAdaWellLocation(rc, loc, HQLOC);
					if (wellLoc == null || loc.distanceSquaredTo(HQLOC) < wellLoc.distanceSquaredTo(HQLOC)) {
						if (rc.getID() % RunCarrier.CARRIER_DIFF_MOD == 0 && !RunCarrier.onBanList(loc))
							wellLoc = loc;
					}
					board[loc.x][loc.y] = M_ADA;
					break;
				case ELIXIR:	
					board[loc.x][loc.y] = M_ELIX;
					break;
			}
        }

        length = islands.length;
		for (int i = length; --i >= 0;) {
            int id = islands[i];
            MapLocation[] islandLocs = rc.senseNearbyIslandLocations(id);
			Team team = rc.senseTeamOccupyingIsland(id);
           	Communication.updateIslandInfo(rc, id);
            int _length = islandLocs.length;
			for (int j = _length; --j >= 0;) {
                MapLocation loc = islandLocs[j];
                switch (team) {
                    case A:
                        board[loc.x][loc.y] = M_AISL;
                        break;
                    case B:
                        board[loc.x][loc.y] = M_BISL;
                        break;
                    default:
                        board[loc.x][loc.y] = M_NISL;
                }
            }
		}
        Communication.tryWriteMessages(rc);
	}

	
	// Navigation
	static void navigateTo(RobotController rc, MapLocation loc) throws GameActionException {
		bug2(rc, loc);
		if (rc.getType() == RobotType.CARRIER) {
			bug2(rc, loc);
		}
	}

    static boolean hasObstacle(RobotController rc, MapLocation loc) throws GameActionException {
        if (!rc.onTheMap(loc))
            return true;
        int tile = board[loc.x][loc.y];
        if (tile == M_AHQ || tile == M_BHQ || tile == M_STORM)
            return true;
        return false;
    }    
	
    static boolean tryMove(RobotController rc, Direction dir) throws GameActionException {
        MapLocation me = rc.getLocation();
        if (!hasObstacle(rc, me.add(dir)) && rc.canMove(dir)) {
            rc.move(dir);
			return true;
        } else {
            Direction right = dir.rotateRight();
			if (!hasObstacle(rc, me.add(dir)) && rc.canMove(right)) {
				rc.move(right);
				return true;
			}
            Direction left = dir.rotateLeft();
			if (!hasObstacle(rc, me.add(dir)) && rc.canMove(left)) {
				rc.move(left);
				return true;
			}
		}
		return false;	
	}

	static void turnRight() throws GameActionException {
		currentDirection = currentDirection.rotateRight();
	}	

	static void turnLeft() throws GameActionException {
        currentDirection = currentDirection.rotateLeft();
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
		Direction senseDir = currentDirection.rotateRight().rotateRight(); 
		MapLocation tile = rc.getLocation().add(senseDir);
        return hasObstacle(rc, tile);
	}

	static boolean senseFrontRight(RobotController rc) throws GameActionException {
		Direction senseDir = currentDirection.rotateRight(); 
		MapLocation tile = rc.getLocation().add(senseDir);
        return hasObstacle(rc, tile);
	}

	static boolean senseFront(RobotController rc) throws GameActionException {
		MapLocation tile = rc.getLocation().add(currentDirection);
        return hasObstacle(rc, tile);
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
			currentDirection = goalDir;
			return;
        } else {
			if (!onObstacle) {
				MapLocation pathTile = rc.getLocation().add(goalDir);
				if (board[pathTile.x][pathTile.y] == M_STORM || rng.nextInt(3) == 1) { // indicates obstacle
					onObstacle = true;
					currentDirection = goalDir;
					turnLeft();
					goalDir = currentDirection;
					if (rc.canMove(goalDir)) {
						rc.move(goalDir);
					}
				}
			} else {
				rc.setIndicatorString("on obstacle");
				// follow obstacle using right hand rule
				boolean frontRight = senseFrontRight(rc);
				if (!frontRight) {
                    currentDirection = currentDirection.rotateRight();
					goalDir = currentDirection;
					tryMove(rc, goalDir);
				}
				boolean front = senseFront(rc);
				if (!front) {
					goalDir = currentDirection;
					tryMove(rc, goalDir);
				} else {
                    currentDirection = currentDirection.rotateLeft();
				}

			}
		}
	}
	static boolean touchingObstacle(RobotController rc) throws GameActionException {
		MapLocation me = rc.getLocation();
        Direction rightHandDir = currentDirection.rotateRight().rotateRight();
        return hasObstacle(rc, me.add(rightHandDir)) || hasObstacle(rc, me.add(rightHandDir.rotateRight())) || hasObstacle(rc, me.add(rightHandDir.rotateLeft()));
	}

	static MapLocation startPoint;
	static MapLocation goalLoc;
	static MapLocation hitPoint;
	static float slope;
	static float yIntercept;
	static boolean wallMode = false;

	static void bug2(RobotController rc, MapLocation loc) throws GameActionException {
		//write this path finding algorithm based on the following pseudocode
		// 		On a high level, the Bug2 algorithm has two main modes:

		// Go to Goal Mode: Move from the current location towards the goal (x,y) coordinate.
		// Wall Following Mode: Move along a wall.
		// Here is pseudocode for the algorithm:

		// 1.      Calculate a start-goal line. The start-goal line is an imaginary line that connects the starting position to the goal position.

		// 2.      While Not at the Goal

		// Move towards the goal along the start-goal line.
		// If a wall is encountered:
		// Remember the location where the wall was first encountered. This is the “hit point.”
		// Follow the wall until you encounter the start-goal line. This point is known as the “leave point.”
		//  If the leave point is closer to the goal than the hit point, leave the wall, and move towards the goal again.
		// Otherwise, continue following the wall.
		// 3.      When the goal is reached, stop.
		navCount++;
		int dist = rc.getLocation().distanceSquaredTo(loc);
		if (dist < 1) {
			return;
		}

		// calculate a start-goal line
		if (goalLoc == null || !goalLoc.equals(loc)) {
			goalLoc = loc;
			currentDirection = rc.getLocation().directionTo(goalLoc);
			startPoint = rc.getLocation();
			slope = (goalLoc.y - startPoint.y) / (goalLoc.x - startPoint.x + 0.001f);
			yIntercept = startPoint.y - slope * startPoint.x;
			hitPoint = null;
			wallMode = false;
			navCount = 0;
		}
        rc.setIndicatorLine(startPoint, goalLoc, 0, 0, 255);
		Direction goalDir = rc.getLocation().directionTo(goalLoc);
		// head towards goal
		if (!wallMode && !tryMove(rc, goalDir) && rc.getMovementCooldownTurns() == 0) {
			// if we're in this block, we couldn't move in the direction of the goal
            MapLocation pathTile = rc.getLocation().add(goalDir);
            if (hasObstacle(rc, pathTile)) {
			    wallMode = true;
                currentDirection = currentDirection.rotateLeft().rotateLeft();
			    hitPoint = rc.getLocation();
            } else {
                bugRandom(rc, goalLoc);
            }
		}
		

		if (wallMode) {
            rc.setIndicatorLine(startPoint, goalLoc, 255, 0, 0);

			// check if we are on the line

            if (onMLine(rc.getLocation())) {
			    rc.setIndicatorString("On m-line: " + rc.getLocation().distanceSquaredTo(goalLoc) + " < " + hitPoint.distanceSquaredTo(goalLoc) + "?"); 
            }
			if (onMLine(rc.getLocation()) && rc.getLocation().distanceSquaredTo(goalLoc) < hitPoint.distanceSquaredTo(goalLoc)) {
				// if we are on the line, we are done wall following
				wallMode = false;
                rc.setIndicatorString("left wall");
				return;
			}

			// follow obstacle using right hand rule
			boolean frontRight = senseFrontRight(rc);
			boolean front = senseFront(rc);
            boolean right = senseRight(rc);
			if (!right) {
				Direction moveDirection = currentDirection.rotateRight().rotateRight();
				if (tryMove(rc, moveDirection)) {
                    currentDirection = moveDirection;
                    rc.setIndicatorString("turning right " + currentDirection);
                } else if (rc.getMovementCooldownTurns() == 0) {
                    rc.setIndicatorString("could not turn right " + moveDirection);
                    bugRandom(rc, goalLoc);
                }
            } else if (!frontRight) {
				Direction moveDirection = currentDirection.rotateRight();
				if (tryMove(rc, moveDirection)) {
                    currentDirection = moveDirection;
                    rc.setIndicatorString("turning front right " + currentDirection);
                } else if (rc.getMovementCooldownTurns() == 0){
                    rc.setIndicatorString("could not turn front right " + moveDirection);
                    bugRandom(rc, goalLoc);
                }
			} else if (!front) {
                if (tryMove(rc, currentDirection))
                    rc.setIndicatorString("moving forward " + currentDirection);
			} else {
				turnLeft();
                rc.setIndicatorString("turning left " + currentDirection);
			}
		}
        rc.setIndicatorLine(rc.getLocation(), rc.getLocation().add(currentDirection), 0, 255, 0);
        if (!touchingObstacle(rc) || navCount > 50)
            goalLoc = null;
	}

	static boolean onMLine(MapLocation loc) {
		float epsilon = 2.5f;
		return abs(loc.y - (slope * loc.x + yIntercept)) < epsilon;
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
            } else {
				Clock.yield();
			}
		}	
		i = 0;
		while (i < 4) {
            rc.setIndicatorString("Trying to build a carrier");
			MapLocation loc = getSpawnLocation(rc, RobotType.CARRIER);
            if (loc != null) {
                rc.buildRobot(RobotType.CARRIER, loc);
				i++;
            } else {
				Clock.yield();
			}
		}	
	}
	
	static int getTotalResources(RobotController rc) throws GameActionException {
		return rc.getResourceAmount(ResourceType.ADAMANTIUM) + rc.getResourceAmount(ResourceType.MANA) + rc.getResourceAmount(ResourceType.ELIXIR);
	}

	static int abs(int x) {
		return x < 0 ? -x : x;
	}

	static float abs(float x) {
		return x < 0 ? -x : x;
	}
	static MapLocation getClosestControlledIsland (RobotController rc) throws GameActionException {
		// this will find the closest loc
		int bestDist = maxDistSquared;
		MapLocation bestLoc = null;
		for (int index = GameConstants.MAX_NUMBER_ISLANDS; --index >= 0;) {
			int id = index + 1;
			if (readTeamHoldingIsland(rc, id).equals(myTeam)) {
				MapLocation currLoc = readIslandLocation(rc, id);
				int currDist = currLoc.distanceSquaredTo(rc.getLocation());
				if (currDist < bestDist) {
					bestDist = currDist;
					bestLoc = currLoc;
				}
			}
		}
		return bestLoc;
	}

	static MapLocation getClosestEnemyIsland (RobotController rc) throws GameActionException {
		// this will find the closest loc
		int bestDist = maxDistSquared;
		MapLocation bestLoc = null;
		for (int index = GameConstants.MAX_NUMBER_ISLANDS; --index >= 0;) {
			int id = index + 1;
			if (readTeamHoldingIsland(rc, id).equals(enemyTeam)) {
				MapLocation currLoc = readIslandLocation(rc, id);
				if (currLoc == null)
					continue;
				int currDist = currLoc.distanceSquaredTo(rc.getLocation());
				if (currDist < bestDist) {
					bestDist = currDist;
					bestLoc = currLoc;
				}
			}
		}
		return bestLoc;
	}
}
