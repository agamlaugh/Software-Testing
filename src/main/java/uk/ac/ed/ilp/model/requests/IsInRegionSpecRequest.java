package uk.ac.ed.ilp.model.requests;

import uk.ac.ed.ilp.model.LngLat;
import uk.ac.ed.ilp.model.Region;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@NoArgsConstructor
public class IsInRegionSpecRequest {
    private LngLat position;
    private Region region;
}
