package uk.ac.ed.ilp.model.requests;

import uk.ac.ed.ilp.model.LngLat;
import uk.ac.ed.ilp.model.Region;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class IsInRegionSpecRequest {
    private LngLat position;
    private Region region;

    public IsInRegionSpecRequest() {}

    public LngLat getPosition() { return position; }
    public void setPosition(LngLat position) { this.position = position; }

    public Region getRegion() { return region; }
    public void setRegion(Region region) { this.region = region; }
}
