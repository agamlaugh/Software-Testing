package uk.ac.ed.ilp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

/**
 * Medical dispatch record
 * Required: id, capacity, delivery
 * Optional: date, time, cooling, heating, maxCost
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@NoArgsConstructor
public class MedDispatchRec {
    private Integer id; // Required
    private String date; // Optional (format: "2025-12-22")
    private String time; // Optional (format: "14:30" or "14:30:00")
    private MedDispatchRequirements requirements;
    private LngLat delivery; 
}

