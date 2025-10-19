package uk.ac.ed.ilp.service;

import uk.ac.ed.ilp.model.LngLat;
import org.springframework.stereotype.Service;

/**
 * Service for handling distance-related calculations between geographic points.
 * Provides methods for calculating Euclidean distance and checking proximity.
 */
@Service
public class DistanceService {
    
    private static final double PROXIMITY_THRESHOLD = 0.00015;
    
    /**
     * Calculates the Euclidean distance between two geographic points.
     * 
     * @param position1 First position
     * @param position2 Second position
     * @return Euclidean distance in degrees
     */
    public double calculateDistance(LngLat position1, LngLat position2) {
        double dLat = position1.getLat() - position2.getLat();
        double dLng = position1.getLng() - position2.getLng();
        return Math.sqrt(dLat * dLat + dLng * dLng);
    }
    
    /**
     * Checks if two positions are close to each other within the proximity threshold.
     * 
     * @param position1 First position
     * @param position2 Second position
     * @return true if positions are within proximity threshold, false otherwise
     */
    public boolean areClose(LngLat position1, LngLat position2) {
        double distance = calculateDistance(position1, position2);
        return distance < PROXIMITY_THRESHOLD;
    }
    
    /**
     * Gets the proximity threshold used for "close" calculations.
     * 
     * @return proximity threshold in degrees
     */
    public double getProximityThreshold() {
        return PROXIMITY_THRESHOLD;
    }
}
