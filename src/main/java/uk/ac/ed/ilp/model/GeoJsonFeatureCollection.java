package uk.ac.ed.ilp.model;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * GeoJSON FeatureCollection structure
 * Used for calcDeliveryPathAsGeoJson endpoint
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GeoJsonFeatureCollection {
    private String type = "FeatureCollection";
    private List<GeoJsonFeature> features;
}

