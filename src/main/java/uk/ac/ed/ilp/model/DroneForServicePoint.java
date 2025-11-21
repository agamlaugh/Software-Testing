package uk.ac.ed.ilp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Drones available at a service point
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@NoArgsConstructor
public class DroneForServicePoint {
    private Integer servicePointId;
    private List<DroneAvailabilityInfo> drones;
}

