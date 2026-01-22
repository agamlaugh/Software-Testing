package uk.ac.ed.ilp.unit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.ac.ed.ilp.model.LngLat;
import uk.ac.ed.ilp.service.DistanceService;

import static org.assertj.core.api.Assertions.assertThat;

class DistanceServiceTest {

    private final DistanceService distanceService = new DistanceService();

    @Test
    @DisplayName("calculateDistance: Pythagorean distance matches expected")
    void calculateDistance_matchesExpected() {
        double distance = distanceService.calculateDistance(
                new LngLat(0.0, 0.0),
                new LngLat(3.0, 4.0)
        );
        assertThat(distance).isEqualTo(5.0);
    }

    @Test
    @DisplayName("areClose: true when within threshold, false when beyond threshold")
    void areClose_respectsThreshold() {
        LngLat origin = new LngLat(0.0, 0.0);
        assertThat(distanceService.areClose(origin, new LngLat(0.000149, 0.0))).isTrue();
        assertThat(distanceService.areClose(origin, new LngLat(0.00016, 0.0))).isFalse();
    }
}
