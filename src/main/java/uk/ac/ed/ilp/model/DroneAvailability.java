package uk.ac.ed.ilp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

/**
 * Drone availability time slot
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@NoArgsConstructor
public class DroneAvailability {
    private String dayOfWeek; // MONDAY, TUESDAY, etc.
    private String from; // "00:00:00" format
    private String until; // "23:59:59" format
}

