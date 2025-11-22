package uk.ac.ed.ilp.model;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * Response structure for calcDeliveryPath endpoint
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryPathResponse {
    private Double totalCost;
    private Integer totalMoves;
    private List<DronePath> dronePaths;
}

