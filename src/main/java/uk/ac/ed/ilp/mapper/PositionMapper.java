package uk.ac.ed.ilp.mapper;

import uk.ac.ed.ilp.model.LngLat;
import uk.ac.ed.ilp.model.requests.DistanceRequest;
import uk.ac.ed.ilp.model.requests.NextPositionRequest;
import uk.ac.ed.ilp.model.requests.IsInRegionSpecRequest;
import uk.ac.ed.ilp.model.Region;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting between different data transfer objects
 * Following the DTO pattern from Lecture
 */
@Component
public class PositionMapper {

    /**
     * Creates a DistanceRequest DTO from two LngLat positions
     */
    public DistanceRequest createDistanceRequest(LngLat position1, LngLat position2) {
        DistanceRequest request = new DistanceRequest();
        request.setPosition1(position1);
        request.setPosition2(position2);
        return request;
    }

    /**
     * Creates a NextPositionRequest DTO from start position and angle
     */
    public NextPositionRequest createNextPositionRequest(LngLat start, Double angle) {
        NextPositionRequest request = new NextPositionRequest();
        request.setStart(start);
        request.setAngle(angle);
        return request;
    }

    /**
     * Creates an IsInRegionSpecRequest DTO from position and region
     */
    public IsInRegionSpecRequest createIsInRegionRequest(LngLat position, Region region) {
        IsInRegionSpecRequest request = new IsInRegionSpecRequest();
        request.setPosition(position);
        request.setRegion(region);
        return request;
    }

    /**
     * Validates and creates a LngLat from coordinates
     * Throws InvalidCoordinatesException if coordinates are invalid
     */
    public LngLat createValidLngLat(double lng, double lat) {
        LngLat lngLat = new LngLat(lng, lat);
        if (!lngLat.isValid()) {
            throw new IllegalArgumentException("Invalid coordinates: lng=" + lng + ", lat=" + lat);
        }
        return lngLat;
    }

    /**
     * Creates a region with validation
     * Throws InvalidRegionException if region is invalid
     */
    public Region createValidRegion(String name, java.util.List<LngLat> vertices) {
        if (vertices == null || vertices.isEmpty()) {
            throw new IllegalArgumentException("Region vertices cannot be null or empty");
        }
        
        Region region = new Region();
        region.setName(name);
        region.setVertices(vertices);
        
        // Validate that polygon is closed
        LngLat first = vertices.get(0);
        LngLat last = vertices.get(vertices.size() - 1);
        if (first.getLng() != last.getLng() || first.getLat() != last.getLat()) {
            throw new IllegalArgumentException("Region polygon must be closed");
        }
        
        return region;
    }

    /**
     * Converts angle to normalized range (0-360)
     */
    public int normalizeAngle(int angle) {
        return ((angle % 360) + 360) % 360;
    }

    /**
     * Creates a copy of LngLat with validation
     */
    public LngLat copyLngLat(LngLat original) {
        if (original == null) {
            throw new IllegalArgumentException("Original LngLat cannot be null");
        }
        return new LngLat(original.getLng(), original.getLat());
    }
}
