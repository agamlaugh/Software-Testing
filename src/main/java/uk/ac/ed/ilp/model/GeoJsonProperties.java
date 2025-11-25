package uk.ac.ed.ilp.model;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * GeoJSON Feature properties
 * Can include metadata about the path, restricted areas, service points, and delivery points
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GeoJsonProperties {
    private String droneId;
    private List<Integer> deliveryIds;
    private Integer totalMoves;
    private Double totalCost;
    private String name; // For restricted areas, service points, delivery points
    private String markerColor; // For styling points (geojson.io recognizes this)
    private String markerSize; // For styling points
    private String markerSymbol; // For styling points
    private String type; // "servicePoint", "deliveryPoint", "restrictedArea", "flightPath"
}

