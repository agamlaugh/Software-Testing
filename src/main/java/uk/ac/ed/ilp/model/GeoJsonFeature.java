package uk.ac.ed.ilp.model;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * GeoJSON Feature structure
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GeoJsonFeature {
    private String type = "Feature";
    private GeoJsonGeometry geometry;
    private GeoJsonProperties properties;
}

