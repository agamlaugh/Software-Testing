package uk.ac.ed.ilp.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import uk.ac.ed.ilp.model.LngLat;
import uk.ac.ed.ilp.service.RegionService;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("RegionService Unit Tests")
class RegionServiceTest {

    private RegionService regionService;

    @BeforeEach
    void setUp() {
        regionService = new RegionService();
    }

    // ========== SIMPLE CASES ==========

    @Test
    @DisplayName("contains - Simple case: point inside square")
    void contains_returnsTrue_forPointInsideSquare() {
        // Given
        List<LngLat> vertices = Arrays.asList(
                new LngLat(0.0, 0.0),
                new LngLat(2.0, 0.0),
                new LngLat(2.0, 2.0),
                new LngLat(0.0, 2.0),
                new LngLat(0.0, 0.0)
        );
        LngLat point = new LngLat(1.0, 1.0); // Center of square

        // When
        boolean result = regionService.contains(vertices, point);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("contains - Simple case: point outside square")
    void contains_returnsFalse_forPointOutsideSquare() {
        // Given
        List<LngLat> vertices = Arrays.asList(
                new LngLat(0.0, 0.0),
                new LngLat(2.0, 0.0),
                new LngLat(2.0, 2.0),
                new LngLat(0.0, 2.0),
                new LngLat(0.0, 0.0)
        );
        LngLat point = new LngLat(3.0, 3.0); // Outside square

        // When
        boolean result = regionService.contains(vertices, point);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("contains - Simple case: point on edge")
    void contains_returnsTrue_forPointOnEdge() {
        // Given
        List<LngLat> vertices = Arrays.asList(
                new LngLat(0.0, 0.0),
                new LngLat(2.0, 0.0),
                new LngLat(2.0, 2.0),
                new LngLat(0.0, 2.0),
                new LngLat(0.0, 0.0)
        );
        LngLat point = new LngLat(1.0, 0.0); // On bottom edge

        // When
        boolean result = regionService.contains(vertices, point);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("contains - Simple case: point on vertex")
    void contains_returnsTrue_forPointOnVertex() {
        // Given
        List<LngLat> vertices = Arrays.asList(
                new LngLat(0.0, 0.0),
                new LngLat(2.0, 0.0),
                new LngLat(2.0, 2.0),
                new LngLat(0.0, 2.0),
                new LngLat(0.0, 0.0)
        );
        LngLat point = new LngLat(0.0, 0.0); // On vertex

        // When
        boolean result = regionService.contains(vertices, point);

        // Then
        assertThat(result).isTrue();
    }

    // ========== EXTREME CASES ==========

    @Test
    @DisplayName("contains - Extreme case: point at exact center")
    void contains_returnsTrue_forPointAtExactCenter() {
        // Given
        List<LngLat> vertices = Arrays.asList(
                new LngLat(0.0, 0.0),
                new LngLat(4.0, 0.0),
                new LngLat(4.0, 4.0),
                new LngLat(0.0, 4.0),
                new LngLat(0.0, 0.0)
        );
        LngLat point = new LngLat(2.0, 2.0); // Exact center

        // When
        boolean result = regionService.contains(vertices, point);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("contains - Extreme case: very small polygon")
    void contains_returnsTrue_forPointInVerySmallPolygon() {
        // Given
        List<LngLat> vertices = Arrays.asList(
                new LngLat(0.0, 0.0),
                new LngLat(0.0001, 0.0),
                new LngLat(0.0001, 0.0001),
                new LngLat(0.0, 0.0001),
                new LngLat(0.0, 0.0)
        );
        LngLat point = new LngLat(0.00005, 0.00005); // Inside tiny square

        // When
        boolean result = regionService.contains(vertices, point);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("contains - Extreme case: triangle")
    void contains_returnsTrue_forPointInTriangle() {
        // Given
        List<LngLat> vertices = Arrays.asList(
                new LngLat(0.0, 0.0),
                new LngLat(2.0, 0.0),
                new LngLat(1.0, 2.0),
                new LngLat(0.0, 0.0)
        );
        LngLat point = new LngLat(1.0, 0.5); // Inside triangle

        // When
        boolean result = regionService.contains(vertices, point);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("contains - Extreme case: complex polygon with many vertices")
    void contains_returnsTrue_forPointInComplexPolygon() {
        // Given - Star-shaped polygon
        List<LngLat> vertices = Arrays.asList(
                new LngLat(0.0, 0.0),
                new LngLat(2.0, 0.0),
                new LngLat(1.0, 1.0),
                new LngLat(2.0, 2.0),
                new LngLat(0.0, 2.0),
                new LngLat(1.0, 1.0),
                new LngLat(0.0, 0.0)
        );
        LngLat point = new LngLat(1.0, 1.0); // Inside star

        // When
        boolean result = regionService.contains(vertices, point);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("contains - Extreme case: point very close to edge")
    void contains_returnsTrue_forPointVeryCloseToEdge() {
        // Given
        List<LngLat> vertices = Arrays.asList(
                new LngLat(0.0, 0.0),
                new LngLat(2.0, 0.0),
                new LngLat(2.0, 2.0),
                new LngLat(0.0, 2.0),
                new LngLat(0.0, 0.0)
        );
        LngLat point = new LngLat(1.0, 0.000001); // Very close to bottom edge

        // When
        boolean result = regionService.contains(vertices, point);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("contains - Extreme case: point very close to vertex")
    void contains_returnsTrue_forPointVeryCloseToVertex() {
        // Given
        List<LngLat> vertices = Arrays.asList(
                new LngLat(0.0, 0.0),
                new LngLat(2.0, 0.0),
                new LngLat(2.0, 2.0),
                new LngLat(0.0, 2.0),
                new LngLat(0.0, 0.0)
        );
        LngLat point = new LngLat(0.000001, 0.000001); // Very close to vertex

        // When
        boolean result = regionService.contains(vertices, point);

        // Then
        assertThat(result).isTrue();
    }

    // ========== FAILURE CASES ==========

    @Test
    @DisplayName("contains - Fail case: null vertices")
    void contains_returnsFalse_forNullVertices() {
        // Given
        List<LngLat> vertices = null;
        LngLat point = new LngLat(1.0, 1.0);

        // When
        boolean result = regionService.contains(vertices, point);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("contains - Fail case: null point")
    void contains_returnsFalse_forNullPoint() {
        // Given
        List<LngLat> vertices = Arrays.asList(
                new LngLat(0.0, 0.0),
                new LngLat(2.0, 0.0),
                new LngLat(2.0, 2.0),
                new LngLat(0.0, 2.0),
                new LngLat(0.0, 0.0)
        );
        LngLat point = null;

        // When
        boolean result = regionService.contains(vertices, point);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("contains - Fail case: empty vertices list")
    void contains_returnsFalse_forEmptyVerticesList() {
        // Given
        List<LngLat> vertices = Collections.emptyList();
        LngLat point = new LngLat(1.0, 1.0);

        // When
        boolean result = regionService.contains(vertices, point);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("contains - Fail case: single vertex")
    void contains_returnsFalse_forSingleVertex() {
        // Given
        List<LngLat> vertices = Arrays.asList(new LngLat(0.0, 0.0));
        LngLat point = new LngLat(1.0, 1.0);

        // When
        boolean result = regionService.contains(vertices, point);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("contains - Fail case: two vertices only")
    void contains_returnsFalse_forTwoVerticesOnly() {
        // Given
        List<LngLat> vertices = Arrays.asList(
                new LngLat(0.0, 0.0),
                new LngLat(2.0, 0.0)
        );
        LngLat point = new LngLat(1.0, 1.0);

        // When
        boolean result = regionService.contains(vertices, point);

        // Then
        assertThat(result).isFalse();
    }

    // ========== EDGE CASES ==========

    @Test
    @DisplayName("contains - Edge case: point on horizontal edge")
    void contains_returnsTrue_forPointOnHorizontalEdge() {
        // Given
        List<LngLat> vertices = Arrays.asList(
                new LngLat(0.0, 0.0),
                new LngLat(2.0, 0.0),
                new LngLat(2.0, 2.0),
                new LngLat(0.0, 2.0),
                new LngLat(0.0, 0.0)
        );
        LngLat point = new LngLat(1.0, 0.0); // On horizontal edge

        // When
        boolean result = regionService.contains(vertices, point);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("contains - Edge case: point on vertical edge")
    void contains_returnsTrue_forPointOnVerticalEdge() {
        // Given
        List<LngLat> vertices = Arrays.asList(
                new LngLat(0.0, 0.0),
                new LngLat(2.0, 0.0),
                new LngLat(2.0, 2.0),
                new LngLat(0.0, 2.0),
                new LngLat(0.0, 0.0)
        );
        LngLat point = new LngLat(0.0, 1.0); // On vertical edge

        // When
        boolean result = regionService.contains(vertices, point);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("contains - Edge case: point outside but very close")
    void contains_returnsFalse_forPointOutsideButVeryClose() {
        // Given
        List<LngLat> vertices = Arrays.asList(
                new LngLat(0.0, 0.0),
                new LngLat(2.0, 0.0),
                new LngLat(2.0, 2.0),
                new LngLat(0.0, 2.0),
                new LngLat(0.0, 0.0)
        );
        LngLat point = new LngLat(2.000001, 1.0); // Just outside right edge

        // When
        boolean result = regionService.contains(vertices, point);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("contains - Edge case: point in corner")
    void contains_returnsTrue_forPointInCorner() {
        // Given
        List<LngLat> vertices = Arrays.asList(
                new LngLat(0.0, 0.0),
                new LngLat(2.0, 0.0),
                new LngLat(2.0, 2.0),
                new LngLat(0.0, 2.0),
                new LngLat(0.0, 0.0)
        );
        LngLat point = new LngLat(2.0, 2.0); // Top-right corner

        // When
        boolean result = regionService.contains(vertices, point);

        // Then
        assertThat(result).isTrue();
    }

    // ========== ALGORITHM TESTS ==========

    @Test
    @DisplayName("contains - Algorithm test: ray casting with odd intersections")
    void contains_returnsTrue_forOddIntersections() {
        // Given - L-shaped polygon
        List<LngLat> vertices = Arrays.asList(
                new LngLat(0.0, 0.0),
                new LngLat(2.0, 0.0),
                new LngLat(2.0, 1.0),
                new LngLat(1.0, 1.0),
                new LngLat(1.0, 2.0),
                new LngLat(0.0, 2.0),
                new LngLat(0.0, 0.0)
        );
        LngLat point = new LngLat(0.5, 0.5); // Inside L-shape

        // When
        boolean result = regionService.contains(vertices, point);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("contains - Algorithm test: ray casting with even intersections")
    void contains_returnsFalse_forEvenIntersections() {
        // Given - L-shaped polygon
        List<LngLat> vertices = Arrays.asList(
                new LngLat(0.0, 0.0),
                new LngLat(2.0, 0.0),
                new LngLat(2.0, 1.0),
                new LngLat(1.0, 1.0),
                new LngLat(1.0, 2.0),
                new LngLat(0.0, 2.0),
                new LngLat(0.0, 0.0)
        );
        LngLat point = new LngLat(1.5, 1.5); // Outside L-shape

        // When
        boolean result = regionService.contains(vertices, point);

        // Then
        assertThat(result).isFalse();
    }
}
