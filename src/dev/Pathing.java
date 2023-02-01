package dev;

import battlecode.common.*;
import static dev.RobotPlayer.*;
import java.util.Queue;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

public class Pathing {

    	// pathfinding state
	static Direction currentDirection = Direction.NORTH;
	static boolean onObstacle = false;
	static int maxDistSquared = 7200;
	static int checkPointSquared = maxDistSquared;
	static int navCount = 0;
    static MapLocation startPoint;
    static MapLocation goalLoc;
    static MapLocation hitPoint;
    static float slope;
    static float yIntercept;
    static boolean wallMode = false;
    static MapLocation lastLoc = null;
    static int stuckCounter = 0;
    static Queue<MapLocation> bfsQ =  new LinkedList<MapLocation>();
    static HashSet<MapLocation> visited = new HashSet<>();
    static LinkedList<MapLocation> path = new LinkedList<>();
    static HashMap<MapLocation, MapLocation> parents = new HashMap<>();
    static int currentPathIdx = -1;

    static boolean atWellLoc(RobotController rc, MapLocation wellLoc) {
        if (wellLoc == null) return false;
        MapLocation me = rc.getLocation();
        for (int i = directions.length; --i >= 0;) {
            if (me.add(directions[i]).equals(wellLoc))
                return true;
        }
        if (me.equals(wellLoc))
            return true;
        return false;
    }
    // Navigation
    static void navigateTo(RobotController rc, MapLocation loc) throws GameActionException {
        if (rc.isMovementReady()) {
            bug2(rc, loc);
            if (rc.getType() == RobotType.CARRIER && !atWellLoc(rc, wellLoc)) {
                bug2(rc, loc);
            }
        }
    }

    static void navigateToWithPath(RobotController rc, MapLocation loc, boolean toHQ) throws GameActionException {
        if (path.size() > 0) {
            if (toHQ) {
                System.out.println(currentPathIdx);
                if (currentPathIdx == -1) {
                    currentPathIdx = path.size() - 1;
                }
                MapLocation nextTile = path.get(currentPathIdx);
                // if (rc.canMove(rc.getLocation().directionTo(nextTile))) {
                //     rc.move(rc.getLocation().directionTo(nextTile));
                //     currentPathIdx--;
                // }
                if (tryMove(rc, rc.getLocation().directionTo(nextTile))) {
                    currentPathIdx--;
                }
            }
            else {
                System.out.println(currentPathIdx);
                if (currentPathIdx == 0) {
                    currentPathIdx = 1;
                }
                MapLocation nextTile = path.get(currentPathIdx);
                // if (rc.canMove(rc.getLocation().directionTo(nextTile))) {
                //     rc.move(rc.getLocation().directionTo(nextTile));
                //     if (currentPathIdx < path.size() - 1)
                //         currentPathIdx++;
                // }
                navigateTo(rc, nextTile);
                if (rc.getLocation().equals(nextTile))
                    currentPathIdx++;
                // if (tryMove(rc, rc.getLocation().directionTo(nextTile))) {
                //     if (currentPathIdx < path.size() - 1)
                //         currentPathIdx++;
                // }
            }
        } else if (rc.isMovementReady()) {
            bug2(rc, loc);
            if (rc.getType() == RobotType.CARRIER && !atWellLoc(rc, wellLoc)) {
                bug2(rc, loc);
            }
        }
    }

    static boolean hasObstacle(RobotController rc, Direction dir) throws GameActionException {
        MapLocation loc = rc.getLocation().add(dir);
        if (!rc.onTheMap(loc))
            return true;
        int tile = RobotPlayer.board[loc.x + loc.y * width];
        if (tile == M_AHQ || tile == M_BHQ || tile == M_STORM)
            return true;
        if (rc.getType() == RobotType.CARRIER && rc.getWeight() == 0)
            return false;
        MapInfo tileInfo = rc.senseMapInfo(loc);
        Direction d = tileInfo.getCurrentDirection();
        return d != Direction.CENTER && (d.dx * dir.dx) + (d.dy * dir.dy) <= 0;
    }    
    
    static boolean tryMove(RobotController rc, Direction dir) throws GameActionException {
        if (!hasObstacle(rc, dir) && rc.canMove(dir)) {
            rc.move(dir);
            return true;
        } else {
            Direction right = dir.rotateRight();
            if (!hasObstacle(rc, dir) && rc.canMove(right)) {
                rc.move(right);
                return true;
            }
            Direction left = dir.rotateLeft();
            if (!hasObstacle(rc, dir) && rc.canMove(left)) {
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
        return hasObstacle(rc, senseDir);
    }

    static boolean senseFrontRight(RobotController rc) throws GameActionException {
        Direction senseDir = currentDirection.rotateRight(); 
        return hasObstacle(rc, senseDir);
    }

    static boolean senseFront(RobotController rc) throws GameActionException {
        return hasObstacle(rc, currentDirection);
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
                if (board[pathTile.x + pathTile.y * width] == M_STORM || rng.nextInt(3) == 1) { // indicates obstacle
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
        Direction rightHandDir = currentDirection.rotateRight().rotateRight();
        return hasObstacle(rc, rightHandDir) || hasObstacle(rc, rightHandDir.rotateRight()) || hasObstacle(rc, rightHandDir.rotateLeft());
    }

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
        navCount++;
        rc.setIndicatorLine(startPoint, goalLoc, 0, 0, 255);
        Direction goalDir = rc.getLocation().directionTo(goalLoc);
        // head towards goal
        if (!wallMode && !tryMove(rc, goalDir) && rc.getMovementCooldownTurns() == 0) {
            // if we're in this block, we couldn't move in the direction of the goal
            if (hasObstacle(rc, goalDir)) {
                wallMode = true;
                currentDirection = currentDirection.rotateLeft().rotateLeft();
                hitPoint = rc.getLocation();
            } else {
                //bugRandom(rc, goalLoc);
                Direction a = currentDirection.rotateLeft().rotateLeft();
                if (rc.canMove(a))
                    rc.move(a);
                else {
                    a = currentDirection.rotateRight().rotateRight();
                    if (rc.canMove(a))
                        rc.move(a);
                }
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
        if (!touchingObstacle(rc) || navCount > 200)
            goalLoc = null;
        
        
        if (stuckCounter > 50) {
            if (rc.getType() == RobotType.CARRIER && getTotalResources(rc) == 0) {
                RunCarrier.banWellLoc();
                stuckCounter = 0;
            }
            for (int i = directions.length; --i >= 0;)
                if (rc.canMove(directions[i])) {
                    rc.move(directions[i]);
                    currentDirection = directions[i];
                }
        }
        if (lastLoc == null)
            lastLoc = rc.getLocation();
        
        if (!rc.getLocation().equals(lastLoc)) {
            lastLoc = null;
            stuckCounter = 0;
        } else {
            stuckCounter++;
        }
    }

    static boolean onMLine(MapLocation loc) {
        float epsilon = 3f;
        return abs(loc.y - (slope * loc.x + yIntercept)) < epsilon;
    }

    
    static boolean bfs(RobotController rc, MapLocation src, MapLocation dst) throws GameActionException {
        System.out.println(src + " " + dst);
        if (visited.contains(dst) && path.isEmpty()) {
            MapLocation current = dst;
            while (current != null) {
                path.add(current);
                rc.setIndicatorDot(current, 255, 0, 0);
                current = parents.get(current);
            }
            return true;
        }
        if (bfsQ.size() == 0) {
            rc.setIndicatorDot(src, 255, 0, 0);
            bfsQ.add(src);
            parents.put(src, null);
        }
        if (bfsQ.size() > 0){
            MapLocation next = bfsQ.remove();
            if (visited.contains(next)) {
                return false;
            }
            rc.setIndicatorDot(next, 0, 255, 0);
            for (int i = directions.length; --i >= 0;) {
                MapLocation adj = next.add(directions[i]);
                if (!rc.onTheMap(adj))
                    continue;
                byte tile = board[adj.x + adj.y * width];
                rc.setIndicatorDot(adj, 0, 0, 255);
                if (tile == M_STORM || tile == M_HIDDEN || tile == M_CURE || tile == M_CURN || tile == M_CURS || tile == M_CURW) {
                    visited.add(adj);
                    continue;
                }
                if (!visited.contains(adj) && !parents.containsKey(adj)) {
                    bfsQ.add(adj);
                    parents.put(adj, next);
                }
            }
            visited.add(next);
        }
        return false;
    }
}
