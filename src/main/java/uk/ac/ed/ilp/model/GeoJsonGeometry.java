package uk.ac.ed.ilp.model;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * GeoJSON Geometry structure
 * Supports both LineString and Polygon:
 * - LineString coordinates: [[lng, lat], [lng, lat], ...]
 * - Polygon coordinates: [[[lng, lat], [lng, lat], ...]] (array of rings)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GeoJsonGeometry {
    private String type = "LineString";
    // Use Object to support both LineString (List<List<Double>>) and Polygon (List<List<List<Double>>>)
    private Object coordinates;
}

