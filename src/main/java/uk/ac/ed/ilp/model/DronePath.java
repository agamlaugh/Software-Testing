package uk.ac.ed.ilp.model;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * Path for a single drone
 * Contains drone ID and list of deliveries
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DronePath {
    private String droneId; // Drone ID as String
    private List<DeliveryPath> deliveries; // Array of delivery paths
}

