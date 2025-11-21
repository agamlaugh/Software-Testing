package uk.ac.ed.ilp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

/**
 * Drone capability structure from ILP REST service
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@NoArgsConstructor
public class DroneCapability {
    private Boolean cooling;
    private Boolean heating;
    private Double capacity;
    private Integer maxMoves;
    private Double costPerMove;
    private Double costInitial;
    private Double costFinal;
}

