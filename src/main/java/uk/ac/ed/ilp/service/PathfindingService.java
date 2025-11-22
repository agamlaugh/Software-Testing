package uk.ac.ed.ilp.service;

import org.springframework.stereotype.Service;
import uk.ac.ed.ilp.model.LngLat;
import uk.ac.ed.ilp.model.RestrictedArea;

import java.util.*;

/**
 * Service for pathfinding between points
 * Handles path calculation following compass directions and avoiding restricted areas
 */
@Service
public class PathfindingService {

    private static final double STEP_SIZE = 0.00015;
    private static final double[] COMPASS_DIRECTIONS = {
        0.0, 22.5, 45.0, 67.5, 90.0, 112.5, 135.0, 157.5,
        180.0, 202.5, 225.0, 247.5, 270.0, 292.5, 315.0, 337.5
    };
    private static final double PROXIMITY_THRESHOLD = 0.00015;

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
     * Calculate path from start to end, avoiding restricted areas
     * Backtracking prevention and Obstacle avoidance
     * Returns list of LngLat coordinates representing the path
     */
    public List<LngLat> calculatePath(LngLat start, LngLat end, List<RestrictedArea> restrictedAreas) {
        if (start == null || end == null || !start.isValid() || !end.isValid()) {
            return List.of();
        }

        List<LngLat> path = new ArrayList<>();
        LngLat current = new LngLat(start.getLng(), start.getLat());
        path.add(new LngLat(current.getLng(), current.getLat()));

        // Track visited positions to avoid loops (use rounded coordinates for comparison)
        Set<String> visited = new HashSet<>();
        visited.add(roundPosition(current));

        int maxSteps = 100000; // Safety limit
        int steps = 0;
        int stuckCounter = 0; // Track consecutive stuck moves
        LngLat lastPosition = null;

        while (steps < maxSteps) {
            // Check if we've reached the destination
            if (distanceService.areClose(current, end)) {
                path.add(new LngLat(end.getLng(), end.getLat()));
                return path;
            }

            // Calculate direction to target
            double targetAngle = calculateAngleToTarget(current, end);
            double bestAngle = findBestCompassDirection(targetAngle);
            
            // Try to move in best direction
            LngLat next = positionService.calculateNextPosition(current, bestAngle);
            
            // Check if next position is valid and not visited
            if (isValidPosition(next, restrictedAreas) && !visited.contains(roundPosition(next))) {
                current = next;
                path.add(new LngLat(current.getLng(), current.getLat()));
                visited.add(roundPosition(current));
                stuckCounter = 0;
                lastPosition = null;
            } else {
                // Try alternative directions if blocked or visited
                // Try all directions and pick the best valid one
                LngLat bestCandidate = null;
                double bestCandidateScore = Double.MAX_VALUE;
                
                for (double angle : COMPASS_DIRECTIONS) {
                    LngLat candidate = positionService.calculateNextPosition(current, angle);
                    String candidateKey = roundPosition(candidate);
                    
                    if (isValidPosition(candidate, restrictedAreas) && !visited.contains(candidateKey)) {
                        // Score: prefer directions closer to target, but also consider distance improvement
                        double angleDiff = Math.abs(normalizeAngle(targetAngle - angle));
                        if (angleDiff > 180) {
                            angleDiff = 360 - angleDiff;
                        }
                        double distanceToTarget = distanceService.calculateDistance(candidate, end);
                        double score = angleDiff * 0.5 + distanceToTarget * 0.5; // Combined score
                        
                        if (score < bestCandidateScore) {
                            bestCandidateScore = score;
                            bestCandidate = candidate;
                        }
                    }
                }
                
                if (bestCandidate != null) {
                    current = bestCandidate;
                    path.add(new LngLat(current.getLng(), current.getLat()));
                    visited.add(roundPosition(current));
                    stuckCounter = 0;
                    lastPosition = null;
                } else {
                    // All directions blocked or visited - try backtracking
                    stuckCounter++;
                    if (stuckCounter > 10 && path.size() > 2) {
                        // Backtrack: remove last few positions to try different route
                        int backtrackSteps = Math.min(5, path.size() / 2);
                        for (int i = 0; i < backtrackSteps && path.size() > 1; i++) {
                            LngLat removed = path.remove(path.size() - 1);
                            visited.remove(roundPosition(removed));
                        }
                        if (!path.isEmpty()) {
                            current = path.get(path.size() - 1);
                            stuckCounter = 0;
                            continue;
                        }
                    }
                    
                    // Check if we're making progress
                    if (lastPosition != null && distanceService.areClose(current, lastPosition)) {
                        // Not moving - completely stuck
                        break;
                    }
                    lastPosition = new LngLat(current.getLng(), current.getLat());
                }
            }

            steps++;
        }

        // If we exited the loop without reaching destination, path is incomplete
        return path;
    }
    
    /**
     * Round position to avoid floating point precision issues in visited set
     */
    private String roundPosition(LngLat pos) {
        // Round to 6 decimal places (about 0.1 meter precision)
        double roundedLng = Math.round(pos.getLng() * 1000000.0) / 1000000.0;
        double roundedLat = Math.round(pos.getLat() * 1000000.0) / 1000000.0;
        return roundedLng + "," + roundedLat;
    }

    /**
     * Calculate angle from current position to target
     */
    private double calculateAngleToTarget(LngLat current, LngLat target) {
        double dLng = target.getLng() - current.getLng();
        double dLat = target.getLat() - current.getLat();
        double angle = Math.toDegrees(Math.atan2(dLat, dLng));
        return normalizeAngle(angle);
    }

    /**
     * Find best compass direction closest to target angle
     */
    private double findBestCompassDirection(double targetAngle) {
        double bestAngle = COMPASS_DIRECTIONS[0];
        double minDiff = Double.MAX_VALUE;

        for (double angle : COMPASS_DIRECTIONS) {
            double diff = Math.abs(normalizeAngle(targetAngle - angle));
            if (diff > 180) {
                diff = 360 - diff;
            }
            if (diff < minDiff) {
                minDiff = diff;
                bestAngle = angle;
            }
        }

        return bestAngle;
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

        // Check if position is in any restricted area
        for (RestrictedArea area : restrictedAreas) {
            if (area != null && area.getVertices() != null && !area.getVertices().isEmpty()) {
                if (regionService.contains(area.getVertices(), position)) {
                    return false; // In restricted area
                }
            }
        }

        return true;
    }

    /**
     * Normalize angle to 0-360 range
     */
    private double normalizeAngle(double angle) {
        double normalized = angle % 360.0;
        if (normalized < 0) {
            normalized += 360.0;
        }
        return normalized;
    }

    /**
     * Calculate number of moves in a path
     * Per Piazza: "Please consider 1 move for the hover as well - as each record will be 1 move"
     * Hover is represented by 2 identical coordinates and counts as 1 move
     * Each transition between consecutive points counts as 1 move, even if they're the same (hover)
     */
    public int countMoves(List<LngLat> path) {
        if (path == null || path.size() < 2) {
            return 0;
        }
        // Each transition between consecutive points is 1 move
        // This includes both actual moves (different coordinates) and hovers (same coordinates)
        // Per Piazza: hover (2 identical coordinates) counts as 1 move
        return path.size() - 1;
    }
}

