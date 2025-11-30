package uk.ac.ed.ilp.model;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * Response for the route comparison endpoint
 * Contains both single-drone and multi-drone solutions with comparison stats
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RouteComparisonResponse {
    
    /** Single-drone solution (null if not possible) */
    private DeliveryPathResponse singleDroneSolution;
    
    /** Multi-drone solution (always attempted) */
    private DeliveryPathResponse multiDroneSolution;
    
    /** Comparison statistics and recommendation */
    private ComparisonStats comparison;
    
    /** GeoJSON representation of single-drone solution */
    private GeoJsonFeatureCollection singleDroneGeoJson;
    
    /** GeoJSON representation of multi-drone solution */
    private GeoJsonFeatureCollection multiDroneGeoJson;
}

