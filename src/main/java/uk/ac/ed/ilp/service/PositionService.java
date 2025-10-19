package uk.ac.ed.ilp.service;

import uk.ac.ed.ilp.model.LngLat;
import org.springframework.stereotype.Service;

/**
 * Service for handling position calculations and drone movement.
 * Provides methods for calculating next positions based on current position and angle.
 */
@Service
public class PositionService {
    
    private static final double STEP_SIZE = 0.00015;
    
    /**
     * Calculates the next position of a drone given a start position and movement angle.
     * 
     * @param start Current position of the drone
     * @param angle Movement angle in degrees (0 = East, 90 = North, etc.)
     * @return Next position after moving one step in the specified direction
     */
    public LngLat calculateNextPosition(LngLat start, int angle) {
        double radians = Math.toRadians(angle);
        double newLng = start.getLng() + STEP_SIZE * Math.cos(radians);
        double newLat = start.getLat() + STEP_SIZE * Math.sin(radians);
        return new LngLat(newLng, newLat);
    }
    
    /**
     * Gets the step size used for drone movement calculations.
     * 
     * @return step size in degrees
     */
    public double getStepSize() {
        return STEP_SIZE;
    }
}
