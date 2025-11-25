package uk.ac.ed.ilp.service;

import org.springframework.stereotype.Service;
import uk.ac.ed.ilp.model.LngLat;
import uk.ac.ed.ilp.model.RestrictedArea;

import java.util.*;

/**
 * Service for pathfinding between points
 * Implements A* (A-Star) algorithm with Euclidean heuristic
 */
@Service
public class PathfindingService {

    private static final double STEP_SIZE = 0.00015;
    private static final double[] COMPASS_DIRECTIONS = {
        0.0, 22.5, 45.0, 67.5, 90.0, 112.5, 135.0, 157.5,
        180.0, 202.5, 225.0, 247.5, 270.0, 292.5, 315.0, 337.5
    };
    private static final double SAFETY_BUFFER = 0.00005; // Safety margin around restricted areas

    private final PositionService positionService;
    private final DistanceService distanceService;
    private final RegionService regionService;

    public PathfindingService(PositionService positionService, 
                             DistanceService distanceService,
                             RegionService regionService) {
        this.positionService = positionService;
        this.distanceService = distanceService;
        this.regionService = regionService;
    }

    /**
     * Node class for A* algorithm
     */
    private static class Node implements Comparable<Node> {
        LngLat position;
        Node parent;
        double gCost; // Cost from start (number of moves)
        double hCost; // Heuristic cost to end (Euclidean distance)
        
        public Node(LngLat position, Node parent, double gCost, double hCost) {
            this.position = position;
            this.parent = parent;
            this.gCost = gCost;
            this.hCost = hCost;
        }
        
        public double getFCost() {
            // Weighted A*: f = g + h * 1.1
            // Breaking ties towards the target makes search much faster (less expansion)
            return gCost + hCost * 1.1;
        }
        
        @Override
        public int compareTo(Node other) {
            return Double.compare(this.getFCost(), other.getFCost());
        }
    }

    /**
     * Calculate path from start to end using A* algorithm
     * Returns list of LngLat coordinates representing the path
     */
    public List<LngLat> calculatePath(LngLat start, LngLat end, List<RestrictedArea> restrictedAreas) {
        if (start == null || end == null || !start.isValid() || !end.isValid()) {
            return List.of();
        }

        PriorityQueue<Node> openSet = new PriorityQueue<>();
        Map<String, Double> gScoreMap = new HashMap<>(); // Key: "lng,lat", Value: gCost
        
        // Initialize start node
        if (!isValidPosition(start, restrictedAreas)) {
            return List.of();
        }
        if (!isValidPosition(end, restrictedAreas)) {
            return List.of();
        }

        // H cost must be in same units as G cost (moves)
        // distance / STEP_SIZE gives minimum moves required
        double startH = distanceService.calculateDistance(start, end) / STEP_SIZE;
        Node startNode = new Node(start, null, 0, startH);
        openSet.add(startNode);
        gScoreMap.put(roundPosition(start), 0.0);
        
        // Safety limit to prevent infinite loops or excessively long searches
        int maxNodesExplored = 150000; // Increased from 30000
        int exploredCount = 0;
        
        while (!openSet.isEmpty()) {
            if (exploredCount++ > maxNodesExplored) {
                // If we hit the limit, return empty (failed to find path within limits)
                return List.of();
            }
            
            Node current = openSet.poll();
            
            // Check if reached destination
            if (distanceService.areClose(current.position, end)) {
                return reconstructPath(current, end);
            }
            
            // Explore neighbors (16 compass directions)
            for (double angle : COMPASS_DIRECTIONS) {
                LngLat nextPos = positionService.calculateNextPosition(current.position, angle);
                
                // Check if valid position and valid segment (avoids restricted areas)
                boolean isValidPos = isValidPosition(nextPos, restrictedAreas);
                boolean isValidSeg = isValidPos && isValidPathSegment(current.position, nextPos, restrictedAreas);
                
                if (isValidSeg) {
                    
                    double newGCost = current.gCost + 1; // Each step is 1 move
                    String nextPosKey = roundPosition(nextPos);
                    
                    // If we found a better path to this neighbor, or haven't visited it
                    if (!gScoreMap.containsKey(nextPosKey) || newGCost < gScoreMap.get(nextPosKey)) {
                        gScoreMap.put(nextPosKey, newGCost);
                        // H cost in moves
                        double hCost = distanceService.calculateDistance(nextPos, end) / STEP_SIZE;
                        // Standard A*
                        Node neighbor = new Node(nextPos, current, newGCost, hCost);
                        openSet.add(neighbor);
                    }
                }
            }
        }
        
        return List.of(); // No path found
    }
    
    /**
     * Reconstruct path from end node back to start
     */
    private List<LngLat> reconstructPath(Node endNode, LngLat target) {
        List<LngLat> path = new ArrayList<>();
        // Add the actual target point as the last point (to be precise)
        path.add(target);
        
        Node current = endNode;
        // Skip the first node if it's very close to target (avoid duplicates)
        if (distanceService.areClose(current.position, target)) {
            current = current.parent;
        }
        
        while (current != null) {
            path.add(current.position);
            current = current.parent;
        }
        
        Collections.reverse(path);
        return path;
    }
    
    /**
     * Round position to create a unique key for Map
     * Maps coordinates to a discrete grid based on STEP_SIZE
     * This is crucial for A* performance to prevent exploring infinite micro-variations
     */
    private String roundPosition(LngLat pos) {
        long x = Math.round(pos.getLng() / STEP_SIZE);
        long y = Math.round(pos.getLat() / STEP_SIZE);
        return x + "," + y;
    }

    /**
     * Check if position is valid (not in any restricted area)
     */
    private boolean isValidPosition(LngLat position, List<RestrictedArea> restrictedAreas) {
        if (position == null || !position.isValid()) {
            return false;
        }

        if (restrictedAreas == null || restrictedAreas.isEmpty()) {
            return true;
        }

        for (RestrictedArea area : restrictedAreas) {
            if (area != null && area.getVertices() != null && !area.getVertices().isEmpty()) {
                if (regionService.contains(area.getVertices(), position)) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Check if a path segment intersects with any restricted area
     * Includes safety buffer
     */
    private boolean isValidPathSegment(LngLat start, LngLat end, List<RestrictedArea> restrictedAreas) {
        if (start == null || end == null || !start.isValid() || !end.isValid()) {
            return false;
        }

        if (restrictedAreas == null || restrictedAreas.isEmpty()) {
            return true;
        }

        for (RestrictedArea area : restrictedAreas) {
            if (area != null && area.getVertices() != null && !area.getVertices().isEmpty()) {
                List<LngLat> vertices = area.getVertices();
                
                // Check segment intersection with polygon edges
                for (int i = 0; i < vertices.size(); i++) {
                    LngLat v1 = vertices.get(i);
                    LngLat v2 = vertices.get((i + 1) % vertices.size());
                    
                    if (segmentsIntersect(start, end, v1, v2)) {
                        return false;
                    }
                    
                    if (distanceToSegment(start, end, v1, v2) < SAFETY_BUFFER) {
                        return false;
                    }
                }
                
                // Check midpoints along segment to ensure we don't pass too close
                // (Approximation for checking if segment is inside polygon)
                int numChecks = 3;
                for (int j = 1; j < numChecks; j++) {
                    double t = j / (double) numChecks;
                    LngLat checkPoint = new LngLat(
                        start.getLng() + t * (end.getLng() - start.getLng()),
                        start.getLat() + t * (end.getLat() - start.getLat())
                    );
                    
                    if (regionService.contains(vertices, checkPoint)) {
                        return false;
                    }
                }
            }
        }

        return true;
    }
    
    // --- Geometric Helper Methods ---

    private double distanceToSegment(LngLat p1, LngLat p2, LngLat a, LngLat b) {
        if (segmentsIntersect(p1, p2, a, b)) return 0.0;
        
        double minDist = Double.MAX_VALUE;
        minDist = Math.min(minDist, distanceToSegmentPoint(p1, a, b));
        minDist = Math.min(minDist, distanceToSegmentPoint(p2, a, b));
        minDist = Math.min(minDist, distanceToSegmentPoint(a, p1, p2));
        minDist = Math.min(minDist, distanceToSegmentPoint(b, p1, p2));
        return minDist;
    }
    
    private double distanceToSegmentPoint(LngLat p, LngLat a, LngLat b) {
        double dx = b.getLng() - a.getLng();
        double dy = b.getLat() - a.getLat();
        if (dx == 0 && dy == 0) return distanceService.calculateDistance(p, a);
        
        double px = p.getLng() - a.getLng();
        double py = p.getLat() - a.getLat();
        
        double t = Math.max(0, Math.min(1, (px * dx + py * dy) / (dx * dx + dy * dy)));
        LngLat closest = new LngLat(a.getLng() + t * dx, a.getLat() + t * dy);
        return distanceService.calculateDistance(p, closest);
    }

    private boolean segmentsIntersect(LngLat p1, LngLat p2, LngLat p3, LngLat p4) {
        double d1 = crossProduct(p3, p4, p1);
        double d2 = crossProduct(p3, p4, p2);
        double d3 = crossProduct(p1, p2, p3);
        double d4 = crossProduct(p1, p2, p4);
        
        return ((d1 > 0 && d2 < 0) || (d1 < 0 && d2 > 0)) &&
               ((d3 > 0 && d4 < 0) || (d3 < 0 && d4 > 0));
    }

    private double crossProduct(LngLat a, LngLat b, LngLat c) {
        return (b.getLng() - a.getLng()) * (c.getLat() - a.getLat()) -
               (b.getLat() - a.getLat()) * (c.getLng() - a.getLng());
    }

    public int countMoves(List<LngLat> path) {
        if (path == null || path.size() < 2) {
            return 0;
        }
        return path.size() - 1;
    }
}
