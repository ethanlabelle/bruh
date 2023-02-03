package dev;

import battlecode.common.*;

import static dev.Communication.*;

import java.util.Random;

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
    static Random rng = null;
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
	static final byte M_HIDDEN = 0b0000;
	static final byte M_CLOUD = 0b0001;
	static final byte M_STORM = 0b0010;
	static final byte M_CURW = 0b0011;
	static final byte M_CURN = 0b0100;
	static final byte M_CURS = 0b0101;
	static final byte M_CURE = 0b0110;
	static final byte M_AISL = 0b0111;
	static final byte M_BISL = 0b1000;
	static final byte M_NISL = 0b1001;
	static final byte M_MANA = 0b1010;
	static final byte M_ADA = 0b1011;
	static final byte M_ELIX = 0b1100;
	static final byte M_AHQ = 0b1101;
	static final byte M_BHQ = 0b1110;
	static final byte M_EMPTY = 0b1111;
	static final byte M_CURNW = 0b10000;
	static final byte M_CURSW = 0b10001;
	static final byte M_CURNE = 0b10010;
	static final byte M_CURSE = 0b10011;
	static byte[] board;
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
	static MapLocation lastLocation = null;
	
	// general robot state
    static int turnCount = 0; // number of turns robot has been alive
	static Team myTeam;
	static Team enemyTeam;
	
	static void printBoard() {
		String out = "";
		for (int i = 0; i < width; i++) {
			for (int j = 0; j < height; j++) {
				out = out + board[i + j * width] + " ";
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
		width = rc.getMapWidth();
		height = rc.getMapHeight();
		rng = new Random(rc.getID());
		board = new byte[width * height];
		myTeam = rc.getTeam();
		enemyTeam = myTeam.opponent();
		
		if (rc.getType() == RobotType.HEADQUARTERS) {
			Communication.addHeadquarter(rc);
			HQLOC = rc.getLocation();
			Communication.tryWriteMessages(rc);
			RunHeadquarters.setup(rc);
			Communication.updateHeadquarterInfo(rc);
		} else {
			Communication.updateHeadquarterInfo(rc);
            HQLOC = Communication.getClosestHeadquarters(rc);
		}


		
        while (true) {
			// This code runs during the entire lifespan of the robot, which is why it is in an infinite
            // loop. If we ever leave this loop and return from run(), the robot dies! At the end of the
            // loop, we call Clock.yield(), signifying that we've done everything we want to do.
			
            turnCount++;  // We have now been alive for one more turn!
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
		static void updateMap(RobotController rc) throws GameActionException {
			if (lastLocation == null || !lastLocation.equals(rc.getLocation())) {
				lastLocation = rc.getLocation();
				mapInfos = rc.senseNearbyMapInfos(); // 200 bytecode
				nearbyWells = rc.senseNearbyWells(); // 100 bytecode
			}
			else{
				mapInfos = null;
				nearbyWells = null;
			}
			robotInfos = rc.senseNearbyRobots();
			islands = rc.senseNearbyIslands(); // 200 bytecode

			int length = mapInfos == null ? 0 : mapInfos.length;
			for (int i = length; --i >= 0;) {
				MapInfo mapInf = mapInfos[i];
				MapLocation loc = mapInf.getMapLocation();
				if (board[loc.x + loc.y * width] == M_HIDDEN) {
					if (!rc.sensePassability(loc))
						board[loc.x + loc.y * width] = M_STORM;
					else
						board[loc.x + loc.y * width] = M_EMPTY;
					switch (mapInf.getCurrentDirection()) {
						case NORTH:
							board[loc.x + loc.y * width] = M_CURN;
							break;
						case SOUTH:
							board[loc.x + loc.y * width] = M_CURS;
							break;
						case EAST:
							board[loc.x + loc.y * width] = M_CURE;
							break;
						case WEST:
							board[loc.x + loc.y * width] = M_CURW;
							break;
						case SOUTHWEST:
							board[loc.x + loc.y * width] = M_CURSW;
							break;
						case SOUTHEAST:
							board[loc.x + loc.y * width] = M_CURSE;
							break;
						case NORTHWEST:
							board[loc.x + loc.y * width] = M_CURNW;
							break;
						case NORTHEAST:
							board[loc.x + loc.y * width] = M_CURNE;
							break;
						default:
					}
					if (rc.senseCloud(loc)) {
						board[loc.x + loc.y * width] = M_CLOUD;
					}
				}
			}

		length = robotInfos.length;
		for (int i = length; --i >= 0;) {
			RobotInfo robot = robotInfos[i];
			if (robot.getType() == RobotType.HEADQUARTERS) {
				MapLocation loc = robot.getLocation();
				Team team = robot.getTeam();
				if (team == Team.A) {
					board[loc.x + loc.y * width] = M_AHQ;	
				} else {
					board[loc.x + loc.y * width] = M_BHQ;	
				}
				if (myTeam == team) {
					HQLOC = loc;
				}
				if (myTeam != team && EnemyHQLOC == null){
					EnemyHQLOC = loc;
				}
			}
		}

			length = nearbyWells == null ? 0 : nearbyWells.length;
			for (int i = length; --i >= 0;) {
				WellInfo wellInfo = nearbyWells[i];
				MapLocation arrayLoc;
				MapLocation loc = wellInfo.getMapLocation();
				switch (wellInfo.getResourceType()) {
					case MANA:
						board[loc.x + loc.y * width] = M_MANA;
						arrayLoc = Communication.readManaWellLocation(rc, HQLOC);
						if (arrayLoc == null || loc.distanceSquaredTo(HQLOC) < arrayLoc.distanceSquaredTo(HQLOC))
							Communication.updateManaWellLocation(rc, loc, HQLOC);
						Communication.addManaWell(rc, loc);
						if ((wellLoc == null || loc.distanceSquaredTo(HQLOC) < wellLoc.distanceSquaredTo(HQLOC))) {
                            if (!RunCarrier.onBanList(loc))
							    if (RunCarrier.earlyMana || (rc.getID() % RunCarrier.CARRIER_DIFF_MOD != 0 && !RunCarrier.earlyAda))
								wellLoc = loc;
						}
						break;
					case ADAMANTIUM:
						arrayLoc = Communication.readAdaWellLocation(rc, HQLOC);
						if (arrayLoc == null || loc.distanceSquaredTo(HQLOC) < arrayLoc.distanceSquaredTo(HQLOC))
							Communication.updateAdaWellLocation(rc, loc, HQLOC);
						if (rc.getType() == RobotType.CARRIER && (wellLoc == null || loc.distanceSquaredTo(HQLOC) < wellLoc.distanceSquaredTo(HQLOC))) {
                            if (!RunCarrier.onBanList(loc))
							    if (RunCarrier.earlyAda || (rc.getID() % RunCarrier.CARRIER_DIFF_MOD == 0 && !RunCarrier.earlyMana))
							    	wellLoc = loc;
						}
						board[loc.x + loc.y * width] = M_ADA;
						break;
					case ELIXIR:	
						board[loc.x + loc.y * width] = M_ELIX;
						break;
					default:
						break;
				}
				Communication.tryWriteMessages(rc);
			}

			length = islands.length;
			for (int i = length; --i >= 0;) {
				int id = islands[i];
				Communication.thisIslandLocs = rc.senseNearbyIslandLocations(id);
				Team team = rc.senseTeamOccupyingIsland(id);
				Communication.updateIslandInfo(rc, id);
				int _length = thisIslandLocs.length;
				for (int j = _length; --j >= 0;) {
					MapLocation loc = thisIslandLocs[j];
					switch (team) {
						case A:
							board[loc.x + loc.y * width] = M_AISL;
							break;
						case B:
							board[loc.x + loc.y * width] = M_BISL;
							break;
						default:
							board[loc.x + loc.y * width] = M_NISL;
					}
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
		int bestDist = Pathing.maxDistSquared;
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
		int bestDist = Pathing.maxDistSquared;
		MapLocation bestLoc = null;
		MapLocation myLoc = rc.getLocation();
		for (int index = GameConstants.MAX_NUMBER_ISLANDS; --index >= 0;) {
			int id = index + 1;
			if (readTeamHoldingIsland(rc, id).equals(enemyTeam)) {
				MapLocation currLoc = readIslandLocation(rc, id);
				if (currLoc == null)
					continue;
				int currDist = currLoc.distanceSquaredTo(myLoc);
				if (currDist < bestDist) {
					bestDist = currDist;
					bestLoc = currLoc;
				}
			}
		}
		if (bestLoc == null) {
			int[] islands = rc.senseNearbyIslands();
			for (int i = islands.length; --i >= 0;) {
				if (rc.senseTeamOccupyingIsland(islands[i]) == enemyTeam) {
					MapLocation[] tiles = rc.senseNearbyIslandLocations(islands[i]);
					if (tiles.length > 0) {
						for (int j = tiles.length; --j >= 0;) {
							int currDist = tiles[j].distanceSquaredTo(myLoc);
							if (currDist < bestDist) {
								bestDist = currDist;
								bestLoc = tiles[j];
							}
						} 
					}
				}
			}
		}
		return bestLoc;
	}
}
