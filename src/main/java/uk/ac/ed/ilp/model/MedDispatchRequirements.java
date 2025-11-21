package uk.ac.ed.ilp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

/**
 * Medical dispatch requirements
 * Required: capacity
 * Optional: cooling, heating, maxCost
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@NoArgsConstructor
public class MedDispatchRequirements {
    private Double capacity; // Required
    private Boolean cooling; // Optional, default false if not present
    private Boolean heating; // Optional, default false if not present
    private Double maxCost; // Optional
}

