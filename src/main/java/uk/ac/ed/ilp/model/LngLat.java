package uk.ac.ed.ilp.model;

public class LngLat {
    private double lng;
    private double lat;

    public LngLat() {}

    public LngLat(double lng, double lat) {
        this.lng = lng;
        this.lat = lat;
    }

    public double getLng() { return lng; }
    public void setLng(double lng) { this.lng = lng; }

    public double getLat() { return lat; }
    public void setLat(double lat) { this.lat = lat; }

    public boolean isValid() {
        return lng >= -180.0 && lng <= 180.0 && lat >= -90.0 && lat <= 90.0;
    }
}
