package uk.ac.ed.ilp.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import uk.ac.ed.ilp.model.LngLat;
import uk.ac.ed.ilp.service.DistanceService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.data.Offset.offset;

@DisplayName("DistanceService Unit Tests")
class DistanceServiceTest {

    private DistanceService distanceService;

    @BeforeEach
    void setUp() {
        distanceService = new DistanceService();
    }

    // ========== SIMPLE CASES ==========

    @Test
    @DisplayName("calculateDistance - Simple case: same coordinates")
    void calculateDistance_returnsZero_forSameCoordinates() {
        // Given
        LngLat position1 = new LngLat(0.0, 0.0);
        LngLat position2 = new LngLat(0.0, 0.0);

        // When
        double distance = distanceService.calculateDistance(position1, position2);

        // Then
        assertThat(distance).isEqualTo(0.0);
    }

    @Test
    @DisplayName("calculateDistance - Simple case: known distance")
    void calculateDistance_returnsCorrectDistance_forKnownCoordinates() {
        // Given
        LngLat position1 = new LngLat(0.0, 0.0);
        LngLat position2 = new LngLat(1.0, 0.0); // 1 degree longitude

        // When
        double distance = distanceService.calculateDistance(position1, position2);

        // Then
        assertThat(distance).isEqualTo(1.0);
    }

    @Test
    @DisplayName("areClose - Simple case: points within threshold")
    void areClose_returnsTrue_forPointsWithinThreshold() {
        // Given
        LngLat position1 = new LngLat(0.0, 0.0);
        LngLat position2 = new LngLat(0.0001, 0.0001); // Very close

        // When
        boolean result = distanceService.areClose(position1, position2);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("areClose - Simple case: points outside threshold")
    void areClose_returnsFalse_forPointsOutsideThreshold() {
        // Given
        LngLat position1 = new LngLat(0.0, 0.0);
        LngLat position2 = new LngLat(1.0, 1.0); // Far apart

        // When
        boolean result = distanceService.areClose(position1, position2);

        // Then
        assertThat(result).isFalse();
    }

    // ========== EXTREME CASES ==========

    @Test
    @DisplayName("calculateDistance - Extreme case: maximum longitude difference")
    void calculateDistance_handlesMaximumLongitudeDifference() {
        // Given
        LngLat position1 = new LngLat(-180.0, 0.0);
        LngLat position2 = new LngLat(180.0, 0.0);

        // When
        double distance = distanceService.calculateDistance(position1, position2);

        // Then
        assertThat(distance).isEqualTo(360.0);
    }

    @Test
    @DisplayName("calculateDistance - Extreme case: maximum latitude difference")
    void calculateDistance_handlesMaximumLatitudeDifference() {
        // Given
        LngLat position1 = new LngLat(0.0, -90.0);
        LngLat position2 = new LngLat(0.0, 90.0);

        // When
        double distance = distanceService.calculateDistance(position1, position2);

        // Then
        assertThat(distance).isEqualTo(180.0);
    }

    @Test
    @DisplayName("calculateDistance - Extreme case: very small coordinates")
    void calculateDistance_handlesVerySmallCoordinates() {
        // Given
        LngLat position1 = new LngLat(0.000001, 0.000001);
        LngLat position2 = new LngLat(0.000002, 0.000002);

        // When
        double distance = distanceService.calculateDistance(position1, position2);

        // Then
        assertThat(distance).isCloseTo(0.000001414, offset(0.000000001));
    }

    @Test
    @DisplayName("areClose - Extreme case: exactly at threshold")
    void areClose_returnsFalse_forPointsExactlyAtThreshold() {
        // Given
        LngLat position1 = new LngLat(0.0, 0.0);
        LngLat position2 = new LngLat(0.00015, 0.0); // Exactly at threshold

        // When
        boolean result = distanceService.areClose(position1, position2);

        // Then
        assertThat(result).isFalse(); // Should be false as it's >= threshold
    }

    @Test
    @DisplayName("areClose - Extreme case: just under threshold")
    void areClose_returnsTrue_forPointsJustUnderThreshold() {
        // Given
        LngLat position1 = new LngLat(0.0, 0.0);
        LngLat position2 = new LngLat(0.000149, 0.0); // Just under threshold

        // When
        boolean result = distanceService.areClose(position1, position2);

        // Then
        assertThat(result).isTrue();
    }

    // ========== FAILURE CASES ==========

    @Test
    @DisplayName("calculateDistance - Fail case: null position1")
    void calculateDistance_throwsException_forNullPosition1() {
        // Given
        LngLat position1 = null;
        LngLat position2 = new LngLat(0.0, 0.0);

        // When & Then
        assertThatThrownBy(() -> distanceService.calculateDistance(position1, position2))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("calculateDistance - Fail case: null position2")
    void calculateDistance_throwsException_forNullPosition2() {
        // Given
        LngLat position1 = new LngLat(0.0, 0.0);
        LngLat position2 = null;

        // When & Then
        assertThatThrownBy(() -> distanceService.calculateDistance(position1, position2))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("areClose - Fail case: null position1")
    void areClose_throwsException_forNullPosition1() {
        // Given
        LngLat position1 = null;
        LngLat position2 = new LngLat(0.0, 0.0);

        // When & Then
        assertThatThrownBy(() -> distanceService.areClose(position1, position2))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("areClose - Fail case: null position2")
    void areClose_throwsException_forNullPosition2() {
        // Given
        LngLat position1 = new LngLat(0.0, 0.0);
        LngLat position2 = null;

        // When & Then
        assertThatThrownBy(() -> distanceService.areClose(position1, position2))
                .isInstanceOf(NullPointerException.class);
    }

    // ========== THRESHOLD TESTS ==========

    @Test
    @DisplayName("getProximityThreshold - Returns correct threshold value")
    void getProximityThreshold_returnsCorrectValue() {
        // When
        double threshold = distanceService.getProximityThreshold();

        // Then
        assertThat(threshold).isEqualTo(0.00015);
    }
}
