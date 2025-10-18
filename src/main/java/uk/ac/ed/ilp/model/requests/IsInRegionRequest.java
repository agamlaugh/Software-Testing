package uk.ac.ed.ilp.model.requests;

import uk.ac.ed.ilp.model.LngLat;

public class IsInRegionRequest {
    private String regionName;
    private LngLat point;

    public IsInRegionRequest() {}

    public String getRegionName() { return regionName; }
    public void setRegionName(String regionName) { this.regionName = regionName; }

    public LngLat getPoint() { return point; }
    public void setPoint(LngLat point) { this.point = point; }
}
