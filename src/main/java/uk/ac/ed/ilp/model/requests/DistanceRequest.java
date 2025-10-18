package uk.ac.ed.ilp.model.requests;

import uk.ac.ed.ilp.model.LngLat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DistanceRequest {
    private LngLat position1;
    private LngLat position2;

    public DistanceRequest() {}

    public LngLat getPosition1() { return position1; }
    public void setPosition1(LngLat position1) { this.position1 = position1; }

    public LngLat getPosition2() { return position2; }
    public void setPosition2(LngLat position2) { this.position2 = position2; }
}
