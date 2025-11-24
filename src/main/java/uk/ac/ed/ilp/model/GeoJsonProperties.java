package uk.ac.ed.ilp.model;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * GeoJSON Feature properties
 * Can include metadata about the path
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
}

