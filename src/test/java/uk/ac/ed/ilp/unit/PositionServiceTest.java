package uk.ac.ed.ilp.unit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.ac.ed.ilp.model.LngLat;
import uk.ac.ed.ilp.service.PositionService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class PositionServiceTest {

    private final PositionService positionService = new PositionService();

    @Test
    @DisplayName("calculateNextPosition: normalizes angle and moves one step")
    void calculateNextPosition_normalizesAngle() {
        LngLat start = new LngLat(0.0, 0.0);
        // 450° should normalize to 90° and move north
        LngLat next = positionService.calculateNextPosition(start, 450.0);
        assertThat(next.getLng()).isCloseTo(0.0, within(1e-9));
        assertThat(next.getLat()).isCloseTo(positionService.getStepSize(), within(1e-9));
    }

    @Test
    @DisplayName("calculateNextPosition: supports negative angles")
    void calculateNextPosition_negativeAngle() {
        LngLat start = new LngLat(0.0, 0.0);
        // -90° should normalize to 270° and move south
        LngLat next = positionService.calculateNextPosition(start, -90.0);
        assertThat(next.getLng()).isCloseTo(0.0, within(1e-9));
        assertThat(next.getLat()).isCloseTo(-positionService.getStepSize(), within(1e-9));
    }
}
