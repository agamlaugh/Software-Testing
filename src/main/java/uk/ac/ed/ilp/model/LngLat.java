package uk.ac.ed.ilp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LngLat {
    private double lng;
    private double lat;

    public boolean isValid() {
        return lng >= -180.0 && lng <= 180.0 && lat >= -90.0 && lat <= 90.0;
    }
}
