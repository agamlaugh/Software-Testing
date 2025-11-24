package uk.ac.ed.ilp.model;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * GeoJSON Geometry structure
 * LineString coordinates: [[lng, lat], [lng, lat], ...]
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GeoJsonGeometry {
    private String type = "LineString";
    private List<List<Double>> coordinates; // [[lng, lat], [lng, lat], ...]
}

