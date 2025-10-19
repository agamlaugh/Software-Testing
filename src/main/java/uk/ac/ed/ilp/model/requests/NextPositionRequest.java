package uk.ac.ed.ilp.model.requests;

import uk.ac.ed.ilp.model.LngLat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@NoArgsConstructor
public class NextPositionRequest {
    private LngLat start;
    private Double angle; // degrees
}
