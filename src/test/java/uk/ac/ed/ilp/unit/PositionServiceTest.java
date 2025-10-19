package uk.ac.ed.ilp.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import uk.ac.ed.ilp.model.LngLat;
import uk.ac.ed.ilp.service.PositionService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.data.Offset.offset;

@DisplayName("PositionService Unit Tests")
class PositionServiceTest {

    private PositionService positionService;

    @BeforeEach
    void setUp() {
        positionService = new PositionService();
    }

    // ========== SIMPLE CASES ==========

    @Test
    @DisplayName("calculateNextPosition - Simple case: 0 degrees (East)")
    void calculateNextPosition_returnsCorrectPosition_forZeroAngle() {
        // Given
        LngLat start = new LngLat(0.0, 0.0);
        int angle = 0;

        // When
        LngLat result = positionService.calculateNextPosition(start, angle);

        // Then
        assertThat(result.getLng()).isCloseTo(0.00015, offset(0.000001)); // Should move east
        assertThat(result.getLat()).isCloseTo(0.0, offset(0.000001));
    }

    @Test
    @DisplayName("calculateNextPosition - Simple case: 90 degrees (North)")
    void calculateNextPosition_returnsCorrectPosition_forNinetyDegrees() {
        // Given
        LngLat start = new LngLat(0.0, 0.0);
        int angle = 90;

        // When
        LngLat result = positionService.calculateNextPosition(start, angle);

        // Then
        assertThat(result.getLng()).isCloseTo(0.0, offset(0.000001));
        assertThat(result.getLat()).isCloseTo(0.00015, offset(0.000001)); // Should move north
    }

    @Test
    @DisplayName("calculateNextPosition - Simple case: 180 degrees (West)")
    void calculateNextPosition_returnsCorrectPosition_forOneEightyDegrees() {
        // Given
        LngLat start = new LngLat(0.0, 0.0);
        int angle = 180;

        // When
        LngLat result = positionService.calculateNextPosition(start, angle);

        // Then
        assertThat(result.getLng()).isCloseTo(-0.00015, offset(0.000001)); // Should move west
        assertThat(result.getLat()).isCloseTo(0.0, offset(0.000001));
    }

    @Test
    @DisplayName("calculateNextPosition - Simple case: 270 degrees (South)")
    void calculateNextPosition_returnsCorrectPosition_forTwoSeventyDegrees() {
        // Given
        LngLat start = new LngLat(0.0, 0.0);
        int angle = 270;

        // When
        LngLat result = positionService.calculateNextPosition(start, angle);

        // Then
        assertThat(result.getLng()).isCloseTo(0.0, offset(0.000001));
        assertThat(result.getLat()).isCloseTo(-0.00015, offset(0.000001)); // Should move south
    }

    // ========== EXTREME CASES ==========

    @Test
    @DisplayName("calculateNextPosition - Extreme case: 360 degrees")
    void calculateNextPosition_returnsCorrectPosition_forThreeSixtyDegrees() {
        // Given
        LngLat start = new LngLat(0.0, 0.0);
        int angle = 360;

        // When
        LngLat result = positionService.calculateNextPosition(start, angle);

        // Then
        assertThat(result.getLng()).isCloseTo(0.00015, offset(0.000001)); // Should be same as 0 degrees
        assertThat(result.getLat()).isCloseTo(0.0, offset(0.000001));
    }

    @Test
    @DisplayName("calculateNextPosition - Extreme case: 720 degrees")
    void calculateNextPosition_returnsCorrectPosition_forSevenTwentyDegrees() {
        // Given
        LngLat start = new LngLat(0.0, 0.0);
        int angle = 720;

        // When
        LngLat result = positionService.calculateNextPosition(start, angle);

        // Then
        assertThat(result.getLng()).isCloseTo(0.00015, offset(0.000001)); // Should be same as 0 degrees
        assertThat(result.getLat()).isCloseTo(0.0, offset(0.000001));
    }

    @Test
    @DisplayName("calculateNextPosition - Extreme case: negative angle")
    void calculateNextPosition_returnsCorrectPosition_forNegativeAngle() {
        // Given
        LngLat start = new LngLat(0.0, 0.0);
        int angle = -90;

        // When
        LngLat result = positionService.calculateNextPosition(start, angle);

        // Then
        assertThat(result.getLng()).isCloseTo(0.0, offset(0.000001));
        assertThat(result.getLat()).isCloseTo(-0.00015, offset(0.000001)); // Should move south
    }

    @Test
    @DisplayName("calculateNextPosition - Extreme case: large negative angle")
    void calculateNextPosition_returnsCorrectPosition_forLargeNegativeAngle() {
        // Given
        LngLat start = new LngLat(0.0, 0.0);
        int angle = -450; // -450 = -90 (360 - 450 = -90)

        // When
        LngLat result = positionService.calculateNextPosition(start, angle);

        // Then
        assertThat(result.getLng()).isCloseTo(0.0, offset(0.000001));
        assertThat(result.getLat()).isCloseTo(-0.00015, offset(0.000001)); // Should move south
    }

    @Test
    @DisplayName("calculateNextPosition - Extreme case: very large angle")
    void calculateNextPosition_returnsCorrectPosition_forVeryLargeAngle() {
        // Given
        LngLat start = new LngLat(0.0, 0.0);
        int angle = 1080; // 1080 = 0 (1080 % 360 = 0)

        // When
        LngLat result = positionService.calculateNextPosition(start, angle);

        // Then
        assertThat(result.getLng()).isCloseTo(0.00015, offset(0.000001)); // Should be same as 0 degrees
        assertThat(result.getLat()).isCloseTo(0.0, offset(0.000001));
    }

    @Test
    @DisplayName("calculateNextPosition - Extreme case: 45 degrees")
    void calculateNextPosition_returnsCorrectPosition_forFortyFiveDegrees() {
        // Given
        LngLat start = new LngLat(0.0, 0.0);
        int angle = 45;

        // When
        LngLat result = positionService.calculateNextPosition(start, angle);

        // Then
        // Should move northeast (both lng and lat increase)
        assertThat(result.getLng()).isCloseTo(0.000106, offset(0.000001)); // cos(45°) * 0.00015
        assertThat(result.getLat()).isCloseTo(0.000106, offset(0.000001)); // sin(45°) * 0.00015
    }

    // ========== FAILURE CASES ==========

    @Test
    @DisplayName("calculateNextPosition - Fail case: null start position")
    void calculateNextPosition_throwsException_forNullStartPosition() {
        // Given
        LngLat start = null;
        int angle = 0;

        // When & Then
        assertThatThrownBy(() -> positionService.calculateNextPosition(start, angle))
                .isInstanceOf(NullPointerException.class);
    }

    // ========== STEP SIZE TESTS ==========

    @Test
    @DisplayName("getStepSize - Returns correct step size value")
    void getStepSize_returnsCorrectValue() {
        // When
        double stepSize = positionService.getStepSize();

        // Then
        assertThat(stepSize).isEqualTo(0.00015);
    }

    // ========== PRECISION TESTS ==========

    @Test
    @DisplayName("calculateNextPosition - Precision test: multiple calculations")
    void calculateNextPosition_maintainsPrecision_forMultipleCalculations() {
        // Given
        LngLat current = new LngLat(0.0, 0.0);
        int angle = 0;

        // When - Move 10 steps in the same direction
        for (int i = 0; i < 10; i++) {
            current = positionService.calculateNextPosition(current, angle);
        }

        // Then
        assertThat(current.getLng()).isCloseTo(0.0015, offset(0.000001)); // 10 * 0.00015
        assertThat(current.getLat()).isCloseTo(0.0, offset(0.000001));
    }
}