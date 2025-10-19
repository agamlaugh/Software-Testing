package uk.ac.ed.ilp.service;

import uk.ac.ed.ilp.model.LngLat;
import uk.ac.ed.ilp.model.Region;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for validating input data and request parameters.
 * Provides methods for checking coordinate validity and request structure.
 */
@Service
public class ValidationService {
    
    /**
     * Validates that a request body is not null and contains required fields.
     * 
     * @param request The request object to validate
     * @return true if request is valid, false otherwise
     */
    public boolean isValidRequest(Object request) {
        return request != null;
    }
    
    /**
     * Validates that a position has valid coordinates.
     * 
     * @param position The position to validate
     * @return true if position is valid, false otherwise
     */
    public boolean isValidPosition(LngLat position) {
        return position != null && position.isValid();
    }
    
    /**
     * Validates that both positions in a distance request are valid.
     * 
     * @param position1 First position
     * @param position2 Second position
     * @return true if both positions are valid, false otherwise
     */
    public boolean areValidPositions(LngLat position1, LngLat position2) {
        return isValidPosition(position1) && isValidPosition(position2);
    }
    
    /**
     * Validates that a region has valid vertices and is properly closed.
     * 
     * @param region The region to validate
     * @return true if region is valid, false otherwise
     */
    public boolean isValidRegion(Region region) {
        if (region == null || region.getVertices() == null || region.getVertices().isEmpty()) {
            return false;
        }
        
        List<LngLat> vertices = region.getVertices();
        if (vertices.size() < 3) {
            return false; // Need at least 3 points for a polygon
        }
        
        // Check if polygon is closed (first and last points are the same)
        LngLat first = vertices.get(0);
        LngLat last = vertices.get(vertices.size() - 1);
        return first.getLng() == last.getLng() && first.getLat() == last.getLat();
    }
    
    /**
     * Validates that a region request has valid position and region data.
     * 
     * @param position The position to check
     * @param region The region to check
     * @return true if both position and region are valid, false otherwise
     */
    public boolean isValidRegionRequest(LngLat position, Region region) {
        return isValidPosition(position) && isValidRegion(region);
    }
}
