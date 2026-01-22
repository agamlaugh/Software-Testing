package uk.ac.ed.ilp.unit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.ac.ed.ilp.model.LngLat;
import uk.ac.ed.ilp.service.ValidationService;

import static org.assertj.core.api.Assertions.assertThat;

class ValidationServiceTest {

    private final ValidationService service = new ValidationService();

    @Test
    @DisplayName("isValidRequest: returns true for non-null request")
    void isValidRequest_nonNull() {
        assertThat(service.isValidRequest(new Object())).isTrue();
    }

    @Test
    @DisplayName("isValidRequest: returns false for null request")
    void isValidRequest_null() {
        assertThat(service.isValidRequest(null)).isFalse();
    }

    @Test
    @DisplayName("isValidPosition: returns true for valid position")
    void isValidPosition_valid() {
        LngLat pos = new LngLat(0.0, 0.0);
        assertThat(service.isValidPosition(pos)).isTrue();
    }

    @Test
    @DisplayName("isValidPosition: returns false for null position")
    void isValidPosition_null() {
        assertThat(service.isValidPosition(null)).isFalse();
    }

    @Test
    @DisplayName("isValidPosition: returns false for invalid coordinates")
    void isValidPosition_invalid() {
        LngLat invalid = new LngLat(200.0, 0.0);
        assertThat(service.isValidPosition(invalid)).isFalse();
    }

    @Test
    @DisplayName("areValidPositions: returns true when both valid")
    void areValidPositions_bothValid() {
        LngLat p1 = new LngLat(0.0, 0.0);
        LngLat p2 = new LngLat(1.0, 1.0);
        assertThat(service.areValidPositions(p1, p2)).isTrue();
    }

    @Test
    @DisplayName("areValidPositions: returns false when first is null")
    void areValidPositions_firstNull() {
        LngLat p2 = new LngLat(1.0, 1.0);
        assertThat(service.areValidPositions(null, p2)).isFalse();
    }

    @Test
    @DisplayName("areValidPositions: returns false when second is null")
    void areValidPositions_secondNull() {
        LngLat p1 = new LngLat(0.0, 0.0);
        assertThat(service.areValidPositions(p1, null)).isFalse();
    }

    @Test
    @DisplayName("areValidPositions: returns false when first is invalid")
    void areValidPositions_firstInvalid() {
        LngLat invalid = new LngLat(200.0, 0.0);
        LngLat valid = new LngLat(1.0, 1.0);
        assertThat(service.areValidPositions(invalid, valid)).isFalse();
    }

    @Test
    @DisplayName("isValidAngle: returns true for valid angle")
    void isValidAngle_valid() {
        assertThat(service.isValidAngle(45.0)).isTrue();
    }

    @Test
    @DisplayName("isValidAngle: returns false for null")
    void isValidAngle_null() {
        assertThat(service.isValidAngle(null)).isFalse();
    }

    @Test
    @DisplayName("isValidAngle: returns false for infinite")
    void isValidAngle_infinite() {
        assertThat(service.isValidAngle(Double.POSITIVE_INFINITY)).isFalse();
    }
}
