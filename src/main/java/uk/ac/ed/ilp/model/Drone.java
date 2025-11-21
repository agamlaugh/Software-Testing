package uk.ac.ed.ilp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

/**
 * Drone structure from ILP REST service
 * Note: Drone ID is a String 
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@NoArgsConstructor
public class Drone {
    private String id; // String type per Piazza @142, @148
    private String name;
    private DroneCapability capability;
}

