package uk.ac.ed.ilp.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import uk.ac.ed.ilp.model.LngLat;
import uk.ac.ed.ilp.model.Region;
import uk.ac.ed.ilp.model.requests.DistanceRequest;
import uk.ac.ed.ilp.model.requests.NextPositionRequest;
import uk.ac.ed.ilp.model.requests.IsInRegionSpecRequest;
import uk.ac.ed.ilp.service.ValidationService;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ValidationService Unit Tests")
class ValidationServiceTest {

    private ValidationService validationService;

    @BeforeEach
    void setUp() {
        validationService = new ValidationService();
    }

    // ========== SIMPLE CASES ==========

    @Test
    @DisplayName("isValidRequest - Simple case: valid request")
    void isValidRequest_returnsTrue_forValidRequest() {
        // Given
        DistanceRequest request = new DistanceRequest();

        // When
        boolean result = validationService.isValidRequest(request);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("isValidPosition - Simple case: valid coordinates")
    void isValidPosition_returnsTrue_forValidCoordinates() {
        // Given
        LngLat position = new LngLat(0.0, 0.0);

        // When
        boolean result = validationService.isValidPosition(position);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("areValidPositions - Simple case: both positions valid")
    void areValidPositions_returnsTrue_forBothValidPositions() {
        // Given
        LngLat position1 = new LngLat(0.0, 0.0);
        LngLat position2 = new LngLat(1.0, 1.0);

        // When
        boolean result = validationService.areValidPositions(position1, position2);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("isValidAngle - Simple case: valid compass angle")
    void isValidAngle_returnsTrue_forValidCompassAngle() {
        assertThat(validationService.isValidAngle(45.0)).isTrue();
        assertThat(validationService.isValidAngle(-90.0)).isTrue();
        assertThat(validationService.isValidAngle(360.0)).isTrue();
    }

    @Test
    @DisplayName("isValidAngle - Fail case: null, NaN, or off-grid")
    void isValidAngle_returnsFalse_forInvalidValues() {
        assertThat(validationService.isValidAngle(null)).isFalse();
        assertThat(validationService.isValidAngle(Double.NaN)).isFalse();
        assertThat(validationService.isValidAngle(Double.POSITIVE_INFINITY)).isFalse();
        assertThat(validationService.isValidAngle(10.0)).isFalse();
    }

    @Test
    @DisplayName("isValidRegion - Simple case: valid closed polygon")
    void isValidRegion_returnsTrue_forValidClosedPolygon() {
        // Given
        List<LngLat> vertices = Arrays.asList(
                new LngLat(0.0, 0.0),
                new LngLat(1.0, 0.0),
                new LngLat(1.0, 1.0),
                new LngLat(0.0, 1.0),
                new LngLat(0.0, 0.0) // Closed polygon
        );
        Region region = new Region();
        region.setVertices(vertices);

        // When
        boolean result = validationService.isValidRegion(region);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("isValidRegionRequest - Simple case: valid position and region")
    void isValidRegionRequest_returnsTrue_forValidPositionAndRegion() {
        // Given
        LngLat position = new LngLat(0.5, 0.5);
        List<LngLat> vertices = Arrays.asList(
                new LngLat(0.0, 0.0),
                new LngLat(1.0, 0.0),
                new LngLat(1.0, 1.0),
                new LngLat(0.0, 1.0),
                new LngLat(0.0, 0.0)
        );
        Region region = new Region();
        region.setVertices(vertices);

        // When
        boolean result = validationService.isValidRegionRequest(position, region);

        // Then
        assertThat(result).isTrue();
    }

    // ========== EXTREME CASES ==========

    @Test
    @DisplayName("isValidPosition - Extreme case: boundary coordinates")
    void isValidPosition_returnsTrue_forBoundaryCoordinates() {
        // Given - Test all boundaries
        LngLat[] boundaryPositions = {
                new LngLat(-180.0, -90.0), // Southwest corner
                new LngLat(180.0, -90.0),  // Southeast corner
                new LngLat(180.0, 90.0),   // Northeast corner
                new LngLat(-180.0, 90.0)   // Northwest corner
        };

        // When & Then
        for (LngLat position : boundaryPositions) {
            assertThat(validationService.isValidPosition(position)).isTrue();
        }
    }

    @Test
    @DisplayName("isValidPosition - Extreme case: very small coordinates")
    void isValidPosition_returnsTrue_forVerySmallCoordinates() {
        // Given
        LngLat position = new LngLat(0.000001, 0.000001);

        // When
        boolean result = validationService.isValidPosition(position);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("isValidRegion - Extreme case: triangle polygon")
    void isValidRegion_returnsTrue_forTrianglePolygon() {
        // Given
        List<LngLat> vertices = Arrays.asList(
                new LngLat(0.0, 0.0),
                new LngLat(1.0, 0.0),
                new LngLat(0.5, 1.0),
                new LngLat(0.0, 0.0) // Closed triangle
        );
        Region region = new Region();
        region.setVertices(vertices);

        // When
        boolean result = validationService.isValidRegion(region);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("isValidRegion - Extreme case: complex polygon with many vertices")
    void isValidRegion_returnsTrue_forComplexPolygon() {
        // Given - Create a star-shaped polygon
        List<LngLat> vertices = Arrays.asList(
                new LngLat(0.0, 0.0),
                new LngLat(1.0, 0.0),
                new LngLat(0.5, 0.5),
                new LngLat(1.0, 1.0),
                new LngLat(0.0, 1.0),
                new LngLat(0.5, 0.5),
                new LngLat(0.0, 0.0) // Closed star
        );
        Region region = new Region();
        region.setVertices(vertices);

        // When
        boolean result = validationService.isValidRegion(region);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("hasValidRegionVertices - Detects invalid coordinate")
    void hasValidRegionVertices_returnsFalse_forInvalidCoordinate() {
        Region region = new Region();
        region.setVertices(Arrays.asList(
                new LngLat(0.0, 0.0),
                new LngLat(200.0, 0.0),
                new LngLat(0.0, 0.0)
        ));

        assertThat(validationService.hasValidRegionVertices(region)).isFalse();
    }

    @Test
    @DisplayName("isPolygonClosed - Detects open polygon")
    void isPolygonClosed_returnsFalse_forOpenPolygon() {
        List<LngLat> vertices = Arrays.asList(
                new LngLat(0.0, 0.0),
                new LngLat(1.0, 0.0),
                new LngLat(1.0, 1.0)
        );

        assertThat(validationService.isPolygonClosed(vertices)).isFalse();
    }

    // ========== FAILURE CASES ==========

    @Test
    @DisplayName("isValidRequest - Fail case: null request")
    void isValidRequest_returnsFalse_forNullRequest() {
        // Given
        Object request = null;

        // When
        boolean result = validationService.isValidRequest(request);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("isValidPosition - Fail case: null position")
    void isValidPosition_returnsFalse_forNullPosition() {
        // Given
        LngLat position = null;

        // When
        boolean result = validationService.isValidPosition(position);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("isValidPosition - Fail case: invalid longitude")
    void isValidPosition_returnsFalse_forInvalidLongitude() {
        // Given
        LngLat position = new LngLat(181.0, 0.0); // Invalid longitude

        // When
        boolean result = validationService.isValidPosition(position);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("isValidPosition - Fail case: invalid latitude")
    void isValidPosition_returnsFalse_forInvalidLatitude() {
        // Given
        LngLat position = new LngLat(0.0, 91.0); // Invalid latitude

        // When
        boolean result = validationService.isValidPosition(position);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("isValidPosition - Fail case: longitude too small")
    void isValidPosition_returnsFalse_forLongitudeTooSmall() {
        // Given
        LngLat position = new LngLat(-181.0, 0.0); // Invalid longitude

        // When
        boolean result = validationService.isValidPosition(position);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("isValidPosition - Fail case: latitude too small")
    void isValidPosition_returnsFalse_forLatitudeTooSmall() {
        // Given
        LngLat position = new LngLat(0.0, -91.0); // Invalid latitude

        // When
        boolean result = validationService.isValidPosition(position);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("areValidPositions - Fail case: first position null")
    void areValidPositions_returnsFalse_forFirstPositionNull() {
        // Given
        LngLat position1 = null;
        LngLat position2 = new LngLat(0.0, 0.0);

        // When
        boolean result = validationService.areValidPositions(position1, position2);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("areValidPositions - Fail case: second position null")
    void areValidPositions_returnsFalse_forSecondPositionNull() {
        // Given
        LngLat position1 = new LngLat(0.0, 0.0);
        LngLat position2 = null;

        // When
        boolean result = validationService.areValidPositions(position1, position2);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("areValidPositions - Fail case: first position invalid")
    void areValidPositions_returnsFalse_forFirstPositionInvalid() {
        // Given
        LngLat position1 = new LngLat(181.0, 0.0); // Invalid
        LngLat position2 = new LngLat(0.0, 0.0);

        // When
        boolean result = validationService.areValidPositions(position1, position2);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("areValidPositions - Fail case: second position invalid")
    void areValidPositions_returnsFalse_forSecondPositionInvalid() {
        // Given
        LngLat position1 = new LngLat(0.0, 0.0);
        LngLat position2 = new LngLat(0.0, 91.0); // Invalid

        // When
        boolean result = validationService.areValidPositions(position1, position2);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("isValidRegion - Fail case: null region")
    void isValidRegion_returnsFalse_forNullRegion() {
        // Given
        Region region = null;

        // When
        boolean result = validationService.isValidRegion(region);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("isValidRegion - Fail case: null vertices")
    void isValidRegion_returnsFalse_forNullVertices() {
        // Given
        Region region = new Region();
        region.setVertices(null);

        // When
        boolean result = validationService.isValidRegion(region);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("isValidRegion - Fail case: empty vertices")
    void isValidRegion_returnsFalse_forEmptyVertices() {
        // Given
        Region region = new Region();
        region.setVertices(Collections.emptyList());

        // When
        boolean result = validationService.isValidRegion(region);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("isValidRegion - Fail case: unclosed polygon")
    void isValidRegion_returnsFalse_forUnclosedPolygon() {
        // Given
        List<LngLat> vertices = Arrays.asList(
                new LngLat(0.0, 0.0),
                new LngLat(1.0, 0.0),
                new LngLat(1.0, 1.0),
                new LngLat(0.0, 1.0) // Not closed - missing last point
        );
        Region region = new Region();
        region.setVertices(vertices);

        // When
        boolean result = validationService.isValidRegion(region);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("isValidRegion - Fail case: polygon with only 2 points")
    void isValidRegion_returnsFalse_forPolygonWithTwoPoints() {
        // Given
        List<LngLat> vertices = Arrays.asList(
                new LngLat(0.0, 0.0),
                new LngLat(1.0, 0.0) // Only 2 points - can't form a polygon
        );
        Region region = new Region();
        region.setVertices(vertices);

        // When
        boolean result = validationService.isValidRegion(region);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("isValidRegionRequest - Fail case: null position")
    void isValidRegionRequest_returnsFalse_forNullPosition() {
        // Given
        LngLat position = null;
        Region region = new Region();

        // When
        boolean result = validationService.isValidRegionRequest(position, region);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("isValidRegionRequest - Fail case: null region")
    void isValidRegionRequest_returnsFalse_forNullRegion() {
        // Given
        LngLat position = new LngLat(0.0, 0.0);
        Region region = null;

        // When
        boolean result = validationService.isValidRegionRequest(position, region);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("isValidRegionRequest - Fail case: invalid position")
    void isValidRegionRequest_returnsFalse_forInvalidPosition() {
        // Given
        LngLat position = new LngLat(181.0, 0.0); // Invalid
        Region region = new Region();

        // When
        boolean result = validationService.isValidRegionRequest(position, region);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("isValidRegionRequest - Fail case: region with null vertices")
    void isValidRegionRequest_returnsFalse_forRegionWithNullVertices() {
        // Given
        LngLat position = new LngLat(0.0, 0.0);
        Region region = new Region();
        region.setVertices(null);

        // When
        boolean result = validationService.isValidRegionRequest(position, region);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("isValidRegionRequest - Fail case: region with empty vertices")
    void isValidRegionRequest_returnsFalse_forRegionWithEmptyVertices() {
        // Given
        LngLat position = new LngLat(0.0, 0.0);
        Region region = new Region();
        region.setVertices(Collections.emptyList());

        // When
        boolean result = validationService.isValidRegionRequest(position, region);

        // Then
        assertThat(result).isFalse();
    }
}
