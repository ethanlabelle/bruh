package v15;

import java.util.List;

import java.util.ArrayList;

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
	private static final int ARRAY_SIZE = 64;
	private static final int N_SAVED_WELLS = GameConstants.MAX_STARTING_HEADQUARTERS;

    // Maybe you want to change this based on exact amounts which you can get on turn 1
    static final int STARTING_ISLAND_IDX = GameConstants.MAX_STARTING_HEADQUARTERS;
    private static final int MANA_WELL_IDX = GameConstants.MAX_NUMBER_ISLANDS + GameConstants.MAX_STARTING_HEADQUARTERS;
    private static final int ADA_WELL_IDX = MANA_WELL_IDX + N_SAVED_WELLS; 
    private static final int STARTING_ENEMY_IDX = ADA_WELL_IDX + N_SAVED_WELLS; 

    private static final int TOTAL_BITS = 16;
    private static final int MAPLOC_BITS = 12;
    private static final int TEAM_BITS = 4;
    private static final int TEAM_MASK = 0b1111;


    private static List<Message> messagesQueue = new ArrayList<>();
    private static MapLocation[] headquarterLocs = new MapLocation[GameConstants.MAX_STARTING_HEADQUARTERS];



    static void addHeadquarter(RobotController rc) throws GameActionException {
        MapLocation me = rc.getLocation();
        for (int i = 0; i < GameConstants.MAX_STARTING_HEADQUARTERS; i++) {
            if (rc.readSharedArray(i) == 0) {
                rc.writeSharedArray(i, locationToInt(rc, me));
				headquarterLocs[i] = me;
                break;
            }
        }
    }

    static void updateHeadquarterInfo(RobotController rc) throws GameActionException {
        for (int i = 0; i < GameConstants.MAX_STARTING_HEADQUARTERS; i++) {
       	    headquarterLocs[i] = (intToLocation(rc, rc.readSharedArray(i)));
       	    if (rc.readSharedArray(i) == 0) {
       	        break;
       	    }
       	}
    }

    static void tryWriteMessages(RobotController rc) throws GameActionException {
        messagesQueue.removeIf(msg -> msg.turnAdded + OUTDATED_TURNS_AMOUNT < RobotPlayer.turnCount);
        // Can always write (0, 0), so just checks are we in range to write
        if (rc.canWriteSharedArray(0, 0)) {
            while (messagesQueue.size() > 0 ) {
                Message msg = messagesQueue.remove(0); // Take from front or back?
                if (rc.canWriteSharedArray(msg.idx, msg.value)) {
                    rc.writeSharedArray(msg.idx, msg.value);
                }
            }
        }
    }

    static void updateIslandInfo(RobotController rc, int id) throws GameActionException {
        if (headquarterLocs[0] == null) {
            return;
        }
        MapLocation closestIslandLoc = null;
        int closestDistance = -1;
        MapLocation[] islandLocs = rc.senseNearbyIslandLocations(id);
        for (MapLocation loc : islandLocs) {
            int distance = headquarterLocs[0].distanceSquaredTo(loc);
            if (closestIslandLoc == null || distance < closestDistance) {
                closestDistance = distance;
                closestIslandLoc = loc;
            }
        }
        // Remember reading is cheaper than writing so we don't want to write without knowing if it's helpful
        int idx = id + STARTING_ISLAND_IDX - 1;
        int oldIslandValue = rc.readSharedArray(idx);
        int updatedIslandValue = bitPackIslandInfo(rc, idx, closestIslandLoc);
        if (oldIslandValue != updatedIslandValue) {
            Message msg = new Message(idx, updatedIslandValue, RobotPlayer.turnCount);
            messagesQueue.add(msg);
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
        	messagesQueue.add(msg);
		}
    }

    static void updateAdaWellLocation(RobotController rc, MapLocation wellLoc, MapLocation HQ) throws GameActionException {
		int i = getHQInd(HQ);
		if (i != -1) {
			int wellLocInt = locationToInt(rc, wellLoc);
        	Message msg = new Message(ADA_WELL_IDX + i, wellLocInt, RobotPlayer.turnCount);
        	messagesQueue.add(msg);
		}
    }

    static void clearObsoleteEnemies(RobotController rc) {
        for (int i = STARTING_ENEMY_IDX; i < GameConstants.SHARED_ARRAY_LENGTH; i++) {
            try {
                MapLocation mapLoc = intToLocation(rc, rc.readSharedArray(i));
                if (mapLoc == null) {
                    continue;
                }
                if (rc.canSenseLocation(mapLoc) && rc.senseNearbyRobots(mapLoc, AREA_RADIUS, rc.getTeam().opponent()).length == 0) {
                    Message msg = new Message(i, locationToInt(rc, null), RobotPlayer.turnCount);
                    messagesQueue.add(msg);
                }
            } catch (GameActionException e) {
                continue;
            }

        }
    }

    static void reportEnemy(RobotController rc, MapLocation enemy) {
        int slot = -1;
        for (int i = STARTING_ENEMY_IDX; i < GameConstants.SHARED_ARRAY_LENGTH; i++) {
            try {
                MapLocation prevEnemy = intToLocation(rc, rc.readSharedArray(i));
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
            messagesQueue.add(msg);
        }
    }

    static MapLocation getClosestEnemy(RobotController rc) {
        MapLocation answer = null;
        for (int i = STARTING_ENEMY_IDX; i < GameConstants.SHARED_ARRAY_LENGTH; i++) {
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

    private static int locationToInt(RobotController rc, MapLocation m) {
        if (m == null) {
            return 0;
        }
        return 1 + m.x + m.y * rc.getMapWidth();
    }

    private static MapLocation intToLocation(RobotController rc, int m) {
        if (m == 0) {
            return null;
        }
        m--;
        return new MapLocation(m % rc.getMapWidth(), m / rc.getMapWidth());
    }
}
