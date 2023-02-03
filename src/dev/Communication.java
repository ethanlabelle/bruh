package dev;


import java.util.HashSet;
import battlecode.common.*;

class Message {
    public int idx;
    public int value;
    public int turnAdded;

    Message (int idx, int value, int turnAdded) {
        this.idx = idx;
        this.value = value;
        this.turnAdded = turnAdded;
    }
}

class Communication {

    private static final int OUTDATED_TURNS_AMOUNT = 30;
    private static final int AREA_RADIUS = RobotType.CARRIER.visionRadiusSquared;
	private static final int N_SAVED_WELLS = GameConstants.MAX_STARTING_HEADQUARTERS;

    // Maybe you want to change this based on exact amounts which you can get on turn 1
    static final int STARTING_ISLAND_IDX = GameConstants.MAX_STARTING_HEADQUARTERS;
    private static final int MANA_WELL_IDX = GameConstants.MAX_NUMBER_ISLANDS + GameConstants.MAX_STARTING_HEADQUARTERS;
    private static final int ADA_WELL_IDX = MANA_WELL_IDX + N_SAVED_WELLS; 
    private static final int EXTRA_MANA_IDX = ADA_WELL_IDX + N_SAVED_WELLS;
    private static final int STARTING_ENEMY_IDX = EXTRA_MANA_IDX + N_SAVED_WELLS;
    private static final int N_SAVED_ENEMIES = 64-STARTING_ENEMY_IDX;
    private static final int TOTAL_BITS = 16;
    private static final int MAPLOC_BITS = 12;
    private static final int TEAM_BITS = 4;
    private static final int TEAM_MASK = 0b1111;
    private static final int SYMMETRY_MASK = 0b111;
    public static final int VERTICAL_MASK =   0b111111111111011;
    public static final int HORIZONTAL_MASK = 0b111111111111101;
    public static final int ROTATIONAL_MASK = 0b111111111111110;
    private static final int SYMMETRY_BITS = 3;

    private static final int ENEMY_LOCATION_MASK = 0b111111111111;
    private static final int HQ_FLAG = 1 << 12;
	private static final int MESSAGE_QUEUE_SIZE = 1000;
    private static final int MESSAGE_LIMIT = 20;
    // private static Queue<Message> messagesQueue =  new LinkedList<Message>();
    private static Message[] messagesQueue = new Message[MESSAGE_QUEUE_SIZE];
	private static int head = 0;
	private static int tail = 0;
    public static MapLocation[] headquarterLocs = new MapLocation[GameConstants.MAX_STARTING_HEADQUARTERS];
    
    // a set of headquarterLocs
    public static HashSet<MapLocation> headquarterLocsSet = new HashSet<MapLocation>();
    // set of well locations
    public static HashSet<MapLocation> manaWellLocsSet = new HashSet<>();
    public static MapLocation[] thisIslandLocs = {};

	static void add(Message m) throws GameActionException {
		if (messagesQueue[tail] == null)
			messagesQueue[tail] = m;
		else
		   	System.out.println(messagesQueue[-1]);
		tail = ++tail % MESSAGE_QUEUE_SIZE;	
	}

	static Message pop() throws GameActionException {
		if (messagesQueue[head] == null)
		   	System.out.println(messagesQueue[-1]);
		Message toRet = messagesQueue[head];
		messagesQueue[head] = null;
		head = ++head % MESSAGE_QUEUE_SIZE;
		return toRet;
	}

	static int queueSize() throws GameActionException {
        return head > tail? tail + MESSAGE_QUEUE_SIZE - head: tail - head;
	}

    //messagesQueue.removeIf(msg -> msg.turnAdded + OUTDATED_TURNS_AMOUNT < RobotPlayer.turnCount);
	static void clearOld() throws GameActionException {
		while (messagesQueue[head] != null) {
			if (messagesQueue[head].turnAdded + OUTDATED_TURNS_AMOUNT < RobotPlayer.turnCount)
				pop();	
			else 
				return;
		}
	}
    // static void clearOld() throws GameActionException {
    //     while (messagesQueue.size() > 0) {
    //         Message msg = messagesQueue.peek();
    //         if (msg.turnAdded + OUTDATED_TURNS_AMOUNT < RobotPlayer.turnCount) {
    //             messagesQueue.remove();
    //         } else {
    //             break;
    //         }
    //     }
    // }

    static void initSymmetry(RobotController rc) throws GameActionException {
        int sharedArrayInfo = rc.readSharedArray(0);
        if ((sharedArrayInfo & SYMMETRY_MASK) != 0) {
            return;
        }
        sharedArrayInfo = sharedArrayInfo | 0b111;
        rc.writeSharedArray(0, sharedArrayInfo);
    }

    static void setImpossibleSymmetry(RobotController rc, int symmetry) throws GameActionException {
        /*
         * first bit is vertical, second is horizontal, third is rotational
         * 0b101 means horizontal symmetry is impossible
         * to eliminate horizontal symmetry, call setImpossibleSymmetry(rc, 0b101)
         */
        int sharedArrayInfo = rc.readSharedArray(0);
        sharedArrayInfo = sharedArrayInfo & symmetry;
        Message msg = new Message(0, sharedArrayInfo, RobotPlayer.turnCount);
        add(msg);
    }

    static int getSymmetry(RobotController rc) throws GameActionException {
        int sharedArrayInfo = rc.readSharedArray(0);
        return sharedArrayInfo & SYMMETRY_MASK;
    }

    static boolean getBit(int n, int k) {
        return ((n >> k) & 1) == 1;
    }

    static void addHeadquarter(RobotController rc) throws GameActionException {
        MapLocation me = rc.getLocation();
        // first headquarter contains map symmetry info as well
        int sharedArrayInfo = rc.readSharedArray(0);
        if ((sharedArrayInfo >> SYMMETRY_BITS) == 0) {
            int symmetryInfo = sharedArrayInfo & SYMMETRY_MASK;
            rc.writeSharedArray(0, (locationToInt(rc, me) << SYMMETRY_BITS) | symmetryInfo);
            headquarterLocs[0] = me;
            headquarterLocsSet.add(me);
            return;
        }

        for (int i = 1; i < GameConstants.MAX_STARTING_HEADQUARTERS; i++) {
            if (rc.readSharedArray(i) == 0) {
                rc.writeSharedArray(i, locationToInt(rc, me));
				headquarterLocs[i] = me;
                headquarterLocsSet.add(me);
                break;
            }
        }
    }

    static void updateHeadquarterInfo(RobotController rc) throws GameActionException {
        // first headquarter contains map symmetry info as well
        int sharedArrayInfo = rc.readSharedArray(0);

        if ((sharedArrayInfo >> SYMMETRY_BITS) == 0) {
            return;
        }
        MapLocation loc = intToLocation(rc, sharedArrayInfo >> SYMMETRY_BITS);
        headquarterLocs[0] = loc;
        headquarterLocsSet.add(loc);

        for (int i = 1; i < GameConstants.MAX_STARTING_HEADQUARTERS; i++) {
            if (rc.readSharedArray(i) == 0) {
                break;
            }
            loc = intToLocation(rc, rc.readSharedArray(i));
       	    headquarterLocs[i] = loc;
            headquarterLocsSet.add(loc);
       	}
    }

    static MapLocation getClosestHeadquarters(RobotController rc) throws GameActionException {
		int minDist = 7200;
		MapLocation closestHQ = null;
        MapLocation me = rc.getLocation();
        for (int i = GameConstants.MAX_STARTING_HEADQUARTERS; --i >= 0;) {
            MapLocation loc = headquarterLocs[i];
            // System.out.println("HQ " + i + " is at " + loc);
			if (loc != null && loc.distanceSquaredTo(me) < minDist) {
				minDist = loc.distanceSquaredTo(me);
				closestHQ = loc;
			}
        }
        // System.out.println("THE CLOSEST HQ IS " + closestHQ);
        return closestHQ;
    }

    static void tryWriteMessages(RobotController rc) throws GameActionException {
        clearOld();
        int counter = 0;
        // Can always write (0, 0), so just checks are we in range to write
        if (rc.canWriteSharedArray(0, 0)) {
            while ((head > tail ? tail + MESSAGE_QUEUE_SIZE - head: tail - head) > 0 && counter < MESSAGE_LIMIT) {
                Message msg = pop();
                if (msg.idx == EXTRA_MANA_IDX) {
                    // first check if this well is a duplicate 
                    boolean isDup = false;
                    for (int i = MANA_WELL_IDX; i < MANA_WELL_IDX + N_SAVED_WELLS; i++) {
                        int value = rc.readSharedArray(i);
                        if (value == msg.value) {
                            isDup = true;
                            break;
                        }
                    }
                    if (!isDup) {
                        for (int i = EXTRA_MANA_IDX; i < EXTRA_MANA_IDX + N_SAVED_WELLS; i++) {
                            int value = rc.readSharedArray(i);
                            if (value == msg.value) {
                                isDup = true; 
                                break;
                            }
                        }
                    }
                    if (isDup)
                        continue;
                    // find a good spot
                    // TODO: allow writing to empty spots if we have less than four headquarters
                    for (int i = EXTRA_MANA_IDX; i < EXTRA_MANA_IDX + N_SAVED_WELLS; i++) {
                        int value = rc.readSharedArray(i);
                        if (value == 0 && rc.canWriteSharedArray(i, msg.value)) {
                            rc.writeSharedArray(i, msg.value);
                            break;
                        }
                    }
                } else if (rc.canWriteSharedArray(msg.idx, msg.value)) {
                    rc.writeSharedArray(msg.idx, msg.value);
                }
                counter++;
            }
        }
    }

    static void updateIslandInfo(RobotController rc, int id) throws GameActionException {
        if (headquarterLocs[0] == null) {
            return;
        }
        int idx = id + STARTING_ISLAND_IDX - 1;
        int oldIslandValue = rc.readSharedArray(idx);
        Team teamHolding = rc.senseTeamOccupyingIsland(id);
        if (oldIslandValue == 0 || ((oldIslandValue & TEAM_MASK) != teamHolding.ordinal())) {
            MapLocation closestIslandLoc = null;
            int closestDistance = -1;
            for (MapLocation loc : thisIslandLocs) {
                int distance = headquarterLocs[0].distanceSquaredTo(loc);
                if (closestIslandLoc == null || distance < closestDistance) {
                    closestDistance = distance;
                    closestIslandLoc = loc;
                }
            }
            // Remember reading is cheaper than writing so we don't want to write without knowing if it's helpful
            
            int updatedIslandValue = bitPackIslandInfo(rc, idx, closestIslandLoc);
            if (oldIslandValue != updatedIslandValue) {
                Message msg = new Message(idx, updatedIslandValue, RobotPlayer.turnCount);
                add(msg);
            }
        }
    }

    static int bitPackIslandInfo(RobotController rc, int islandIdx, MapLocation closestLoc) throws GameActionException {
        int islandInt = locationToInt(rc, closestLoc);
        islandInt = islandInt << (TOTAL_BITS - MAPLOC_BITS);
        Team teamHolding = rc.senseTeamOccupyingIsland(islandIdx + 1 - STARTING_ISLAND_IDX); // idx in shared array
        islandInt += teamHolding.ordinal();
        return islandInt;
    }

    static Team readTeamHoldingIsland(RobotController rc, int islandId) throws GameActionException {
        islandId = islandId + STARTING_ISLAND_IDX - 1;
        int islandInt = rc.readSharedArray(islandId);
		if (islandInt == 0) {
        	return Team.NEUTRAL;
		}
        int team = (islandInt & TEAM_MASK);
        return Team.values()[team];
    }

    static MapLocation readIslandLocation(RobotController rc, int islandId) throws GameActionException {
        islandId = islandId + STARTING_ISLAND_IDX - 1;
        int islandInt = rc.readSharedArray(islandId);
        int idx = islandInt >> TEAM_BITS;
        MapLocation loc = intToLocation(rc, idx);
        return loc;
    }

	static int getHQInd(MapLocation HQ) throws GameActionException {
		for (int i = 0; i < GameConstants.MAX_STARTING_HEADQUARTERS; i++) {
			if (headquarterLocs[i] != null && headquarterLocs[i].x == HQ.x && headquarterLocs[i].y == HQ.y) {
				return i;
			}	
		}
		return -1;
	}

	static MapLocation getClosestWell(RobotController rc, ResourceType resource) {
		int start = MANA_WELL_IDX;
		if (resource == ResourceType.ADAMANTIUM) {
			start = ADA_WELL_IDX;
		}
        MapLocation answer = null;
        for (int i = start; i < start + N_SAVED_WELLS; i++) {
            final int value;
            try {
                value = rc.readSharedArray(i);
                final MapLocation m = intToLocation(rc, value);
                if (m != null && (answer == null || rc.getLocation().distanceSquaredTo(m) < rc.getLocation().distanceSquaredTo(answer))) {
                    answer = m;
                }
            } catch (GameActionException e) {
                continue;
            }
        }
        return answer;
    }

    static MapLocation getClosestUnbannedWell(RobotController rc, ResourceType resource) throws GameActionException {
		int start = MANA_WELL_IDX;
		if (resource == ResourceType.ADAMANTIUM) {
			start = ADA_WELL_IDX;
		}
        MapLocation answer = null;
        int value;
        MapLocation m;
        for (int i = start; i < start + N_SAVED_WELLS; i++) {
            value = rc.readSharedArray(i);
            m = intToLocation(rc, value);
            if (m != null && !RunCarrier.onBanList(m) && (answer == null || rc.getLocation().distanceSquaredTo(m) < rc.getLocation().distanceSquaredTo(answer)))
                answer = m;
        }
        if (start == MANA_WELL_IDX) {
            for (int i = EXTRA_MANA_IDX; i < EXTRA_MANA_IDX + N_SAVED_WELLS; i++) {
                value = rc.readSharedArray(i);
                m = intToLocation(rc, value);
                if (m != null && !RunCarrier.onBanList(m) && (answer == null || rc.getLocation().distanceSquaredTo(m) < rc.getLocation().distanceSquaredTo(answer)))
                    answer = m;
            }
        }
        return answer;
    }

	static MapLocation readManaWellLocation(RobotController rc, MapLocation HQ) throws GameActionException {
		int i = getHQInd(HQ);
		if (i != -1) {
			int wellMessage = rc.readSharedArray(MANA_WELL_IDX + i);	
			if (wellMessage != 0) {
				return intToLocation(rc, wellMessage);
			}
		}
		return null;
	}

	static MapLocation readAdaWellLocation(RobotController rc, MapLocation HQ) throws GameActionException {
		int i = getHQInd(HQ);
		if (i != -1) {
			int wellMessage = rc.readSharedArray(ADA_WELL_IDX + i);	
			if (wellMessage != 0) {
				return intToLocation(rc, wellMessage);
			}
		}
		return null;
	}

    static void updateManaWellLocation(RobotController rc, MapLocation wellLoc, MapLocation HQ) throws GameActionException {
		int i = getHQInd(HQ);
		if (i != -1) {
			int wellLocInt = locationToInt(rc, wellLoc);
        	Message msg = new Message(MANA_WELL_IDX + i, wellLocInt, RobotPlayer.turnCount);
        	add(msg);
		}
    }

    static void addManaWell(RobotController rc, MapLocation wellLoc) throws GameActionException {
        int wellLocInt = locationToInt(rc, wellLoc);
        Message msg = new Message(EXTRA_MANA_IDX, wellLocInt, RobotPlayer.turnCount);
        add(msg);
    }

    static void updateAdaWellLocation(RobotController rc, MapLocation wellLoc, MapLocation HQ) throws GameActionException {
		int i = getHQInd(HQ);
		if (i != -1) {
			int wellLocInt = locationToInt(rc, wellLoc);
        	Message msg = new Message(ADA_WELL_IDX + i, wellLocInt, RobotPlayer.turnCount);
        	add(msg);
		}
    }

    static void clearObsoleteEnemies(RobotController rc) {
        for (int i = STARTING_ENEMY_IDX; i < STARTING_ENEMY_IDX + N_SAVED_ENEMIES; i++) {
            try {
                int value = rc.readSharedArray(i);
                value &= ENEMY_LOCATION_MASK;
                MapLocation mapLoc = intToLocation(rc, value);
                if (mapLoc == null) {
                    continue;
                }
                if (rc.canSenseLocation(mapLoc) && rc.senseNearbyRobots(mapLoc, AREA_RADIUS, rc.getTeam().opponent()).length == 0) {
                    Message msg = new Message(i, locationToInt(rc, null), RobotPlayer.turnCount);
                    add(msg);
                }
            } catch (GameActionException e) {
                continue;
            }

        }
    }

    static void reportEnemy(RobotController rc, MapLocation enemy) throws GameActionException {
        int slot = -1;
        for (int i = STARTING_ENEMY_IDX; i < STARTING_ENEMY_IDX + N_SAVED_ENEMIES; i++) {
            try {
                int value = rc.readSharedArray(i);
                value &= ENEMY_LOCATION_MASK;
                MapLocation prevEnemy = intToLocation(rc, value);
                if (prevEnemy == null) {
                    slot = i;
                    break;
                } else if (prevEnemy.distanceSquaredTo(enemy) < AREA_RADIUS) {
                    return;
                }
            } catch (GameActionException e) {
                continue;
            }
        }
        if (slot != -1) {
            Message msg = new Message(slot, locationToInt(rc, enemy), RobotPlayer.turnCount);
            add(msg);
        }
    }

    static void reportEnemyHeadquarters(RobotController rc, MapLocation enemyHQ) throws GameActionException {
        int slot = -1;
        for (int i = STARTING_ENEMY_IDX; i < STARTING_ENEMY_IDX + N_SAVED_ENEMIES; i++) {
            try {
                int value = rc.readSharedArray(i);
                value &= ENEMY_LOCATION_MASK;
                MapLocation prevEnemy = intToLocation(rc, value);
                if (prevEnemy == null) {
                    slot = i;
                    break;
                } else if (prevEnemy.distanceSquaredTo(enemyHQ) < AREA_RADIUS) {
                    return;
                }
            } catch (GameActionException e) {
                continue;
            }
        }
        if (slot != -1) {
            Message msg = new Message(slot, locationToInt(rc, enemyHQ, true), RobotPlayer.turnCount);
            add(msg);
        }
    }

    static MapLocation[] getEnemyHeadquarters(RobotController rc) throws GameActionException {
        MapLocation[] hqs = new MapLocation[GameConstants.MAX_STARTING_HEADQUARTERS];
        int hqCounter = 0;
        for (int i = STARTING_ENEMY_IDX; i < STARTING_ENEMY_IDX + N_SAVED_ENEMIES; i++) {
            int value = rc.readSharedArray(i);
            if ((value & HQ_FLAG) == HQ_FLAG) {
                value &= ENEMY_LOCATION_MASK;
                hqs[hqCounter] = intToLocation(rc, value);
                hqCounter++;
            }
        }
        MapLocation[] toRet = new MapLocation[hqCounter];
        for (int i = 0; i < hqCounter; i++) {
            toRet[i] = hqs[i];
        }
        return toRet;
    }

    static MapLocation getClosestEnemy(RobotController rc) throws GameActionException {
        MapLocation answer = null;
        for (int i = STARTING_ENEMY_IDX; i < STARTING_ENEMY_IDX + N_SAVED_ENEMIES; i++) {
            int value;
            try {
                value = rc.readSharedArray(i);
                value &= ENEMY_LOCATION_MASK;
                final MapLocation m = intToLocation(rc, value);
                if (m != null && (answer == null || rc.getLocation().distanceSquaredTo(m) < rc.getLocation().distanceSquaredTo(answer))) {
                    answer = m;
                }
            } catch (GameActionException e) {
                continue;
            }
        }
        return answer;
    }

    static int locationToInt(RobotController rc, MapLocation m) {
        if (m == null) {
            return 0;
        }
        return 1 + m.x + m.y * rc.getMapWidth();
    }

    private static int locationToInt(RobotController rc, MapLocation m, boolean hq) {
        if (m == null) {
            return 0;
        }
        int out = 1 + m.x + m.y * rc.getMapWidth();
        if (hq)
            return out | HQ_FLAG;
        else
            return out;
    }

    static MapLocation intToLocation(RobotController rc, int m) {
        if (m == 0) {
            return null;
        }
        m--;
        return new MapLocation(m % rc.getMapWidth(), m / rc.getMapWidth());
    }
}
