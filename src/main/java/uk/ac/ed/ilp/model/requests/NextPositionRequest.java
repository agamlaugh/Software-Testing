package uk.ac.ed.ilp.model.requests;

import uk.ac.ed.ilp.model.LngLat;

public class NextPositionRequest {
    private LngLat start;
    private int angle; // degrees

    public NextPositionRequest() {}

    public LngLat getStart() { return start; }
    public void setStart(LngLat start) { this.start = start; }

    public int getAngle() { return angle; }
    public void setAngle(int angle) { this.angle = angle; }
}
