package uk.ac.ed.ilp.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.ac.ed.ilp.controller.ApiController;
import uk.ac.ed.ilp.model.LngLat;
import uk.ac.ed.ilp.model.requests.DistanceRequest;
import uk.ac.ed.ilp.model.requests.NextPositionRequest;
import uk.ac.ed.ilp.model.requests.IsInRegionSpecRequest;
import uk.ac.ed.ilp.model.Region;
import uk.ac.ed.ilp.service.DistanceService;
import uk.ac.ed.ilp.service.PositionService;
import uk.ac.ed.ilp.service.ValidationService;
import uk.ac.ed.ilp.service.RegionService;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mockito-based unit tests for ApiController
 * Demonstrates service isolation and mocking as taught in Lecture 4
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ApiController Mockito Unit Tests")
class ApiControllerMockitoTest {

    @Mock
    private DistanceService distanceService;
    
    @Mock
    private PositionService positionService;
    
    @Mock
    private ValidationService validationService;
    
    @Mock
    private RegionService regionService;
    
    private ApiController apiController;

    @BeforeEach
    void setUp() {
        apiController = new ApiController(regionService, distanceService, positionService, validationService);
    }

    // ========== DISTANCE TO TESTS ==========

    @Test
    @DisplayName("distanceTo - Mockito: delegates to services correctly")
    void distanceTo_delegatesToServicesCorrectly() {
        // Given
        DistanceRequest request = new DistanceRequest();
        LngLat p1 = new LngLat(0.0, 0.0);
        LngLat p2 = new LngLat(1.0, 1.0);
        request.setPosition1(p1);
        request.setPosition2(p2);
        
        when(validationService.isValidRequest(request)).thenReturn(true);
        when(validationService.areValidPositions(p1, p2)).thenReturn(true);
        when(distanceService.calculateDistance(p1, p2)).thenReturn(1.414);

        // When
        var result = apiController.distanceTo(request);

        // Then
        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody()).isEqualTo(1.414);
        
        verify(validationService).isValidRequest(request);
        verify(validationService).areValidPositions(p1, p2);
        verify(distanceService).calculateDistance(p1, p2);
        verifyNoMoreInteractions(validationService, distanceService, positionService, regionService);
    }

    @Test
    @DisplayName("distanceTo - Mockito: returns 400 for invalid request")
    void distanceTo_returns400_forInvalidRequest() {
        // Given
        DistanceRequest request = new DistanceRequest();
        when(validationService.isValidRequest(request)).thenReturn(false);

        // When
        var result = apiController.distanceTo(request);

        // Then
        assertThat(result.getStatusCode().value()).isEqualTo(400);
        assertThat(result.getBody()).isEqualTo("Invalid request body");
        
        verify(validationService).isValidRequest(request);
        verifyNoMoreInteractions(validationService, distanceService, positionService, regionService);
    }

    @Test
    @DisplayName("distanceTo - Mockito: returns 400 for invalid coordinates")
    void distanceTo_returns400_forInvalidCoordinates() {
        // Given
        DistanceRequest request = new DistanceRequest();
        LngLat p1 = new LngLat(0.0, 0.0);
        LngLat p2 = new LngLat(1.0, 1.0);
        request.setPosition1(p1);
        request.setPosition2(p2);
        
        when(validationService.isValidRequest(request)).thenReturn(true);
        when(validationService.areValidPositions(p1, p2)).thenReturn(false);

        // When
        var result = apiController.distanceTo(request);

        // Then
        assertThat(result.getStatusCode().value()).isEqualTo(400);
        assertThat(result.getBody()).isEqualTo("Invalid coordinates");
        
        verify(validationService).isValidRequest(request);
        verify(validationService).areValidPositions(p1, p2);
        verifyNoMoreInteractions(validationService, distanceService, positionService, regionService);
    }

    // ========== IS CLOSE TO TESTS ==========

    @Test
    @DisplayName("isCloseTo - Mockito: delegates to services correctly")
    void isCloseTo_delegatesToServicesCorrectly() {
        // Given
        DistanceRequest request = new DistanceRequest();
        LngLat p1 = new LngLat(0.0, 0.0);
        LngLat p2 = new LngLat(0.0001, 0.0001);
        request.setPosition1(p1);
        request.setPosition2(p2);
        
        when(validationService.isValidRequest(request)).thenReturn(true);
        when(validationService.areValidPositions(p1, p2)).thenReturn(true);
        when(distanceService.areClose(p1, p2)).thenReturn(true);

        // When
        var result = apiController.isCloseTo(request);

        // Then
        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody()).isEqualTo(true);
        
        verify(validationService).isValidRequest(request);
        verify(validationService).areValidPositions(p1, p2);
        verify(distanceService).areClose(p1, p2);
        verifyNoMoreInteractions(validationService, distanceService, positionService, regionService);
    }

    // ========== NEXT POSITION TESTS ==========

    @Test
    @DisplayName("nextPosition - Mockito: delegates to services correctly")
    void nextPosition_delegatesToServicesCorrectly() {
        // Given
        NextPositionRequest request = new NextPositionRequest();
        LngLat start = new LngLat(0.0, 0.0);
        LngLat nextPos = new LngLat(0.00015, 0.0);
        request.setStart(start);
        request.setAngle(0);
        
        when(validationService.isValidRequest(request)).thenReturn(true);
        when(validationService.isValidPosition(start)).thenReturn(true);
        when(positionService.calculateNextPosition(start, 0)).thenReturn(nextPos);

        // When
        var result = apiController.nextPosition(request);

        // Then
        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody()).isEqualTo(nextPos);
        
        verify(validationService).isValidRequest(request);
        verify(validationService).isValidPosition(start);
        verify(positionService).calculateNextPosition(start, 0);
        verifyNoMoreInteractions(validationService, distanceService, positionService, regionService);
    }

    @Test
    @DisplayName("nextPosition - Mockito: returns 400 for invalid position")
    void nextPosition_returns400_forInvalidPosition() {
        // Given
        NextPositionRequest request = new NextPositionRequest();
        LngLat start = new LngLat(200.0, 0.0); // Invalid
        request.setStart(start);
        request.setAngle(0);
        
        when(validationService.isValidRequest(request)).thenReturn(true);
        when(validationService.isValidPosition(start)).thenReturn(false);

        // When
        var result = apiController.nextPosition(request);

        // Then
        assertThat(result.getStatusCode().value()).isEqualTo(400);
        assertThat(result.getBody()).isEqualTo("Invalid coordinates");
        
        verify(validationService).isValidRequest(request);
        verify(validationService).isValidPosition(start);
        verifyNoMoreInteractions(validationService, distanceService, positionService, regionService);
    }

    // ========== IS IN REGION TESTS ==========

    @Test
    @DisplayName("isInRegion - Mockito: delegates to services correctly")
    void isInRegion_delegatesToServicesCorrectly() {
        // Given
        IsInRegionSpecRequest request = new IsInRegionSpecRequest();
        LngLat position = new LngLat(0.5, 0.5);
        Region region = new Region();
        List<LngLat> vertices = Arrays.asList(
            new LngLat(0.0, 0.0), new LngLat(1.0, 0.0), 
            new LngLat(1.0, 1.0), new LngLat(0.0, 1.0), 
            new LngLat(0.0, 0.0)
        );
        region.setVertices(vertices);
        request.setPosition(position);
        request.setRegion(region);
        
        when(validationService.isValidRequest(request)).thenReturn(true);
        when(validationService.isValidPosition(position)).thenReturn(true);
        when(validationService.isValidRegion(region)).thenReturn(true);
        when(regionService.contains(vertices, position)).thenReturn(true);

        // When
        var result = apiController.isInRegion(request);

        // Then
        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody()).isEqualTo(true);
        
        verify(validationService).isValidRequest(request);
        verify(validationService).isValidPosition(position);
        verify(validationService).isValidRegion(region);
        verify(regionService).contains(vertices, position);
        verifyNoMoreInteractions(validationService, distanceService, positionService, regionService);
    }

    @Test
    @DisplayName("isInRegion - Mockito: returns 400 for unclosed polygon")
    void isInRegion_returns400_forUnclosedPolygon() {
        // Given
        IsInRegionSpecRequest request = new IsInRegionSpecRequest();
        LngLat position = new LngLat(0.5, 0.5);
        Region region = new Region();
        List<LngLat> vertices = Arrays.asList(
            new LngLat(0.0, 0.0), new LngLat(1.0, 0.0), 
            new LngLat(1.0, 1.0), new LngLat(0.0, 1.0) // Not closed
        );
        region.setVertices(vertices);
        request.setPosition(position);
        request.setRegion(region);
        
        when(validationService.isValidRequest(request)).thenReturn(true);
        when(validationService.isValidPosition(position)).thenReturn(true);
        when(validationService.isValidRegion(region)).thenReturn(false);

        // When
        var result = apiController.isInRegion(request);

        // Then
        assertThat(result.getStatusCode().value()).isEqualTo(400);
        assertThat(result.getBody()).isEqualTo("Region polygon must be closed");
        
        verify(validationService).isValidRequest(request);
        verify(validationService).isValidPosition(position);
        verify(validationService).isValidRegion(region);
        verifyNoMoreInteractions(validationService, distanceService, positionService, regionService);
    }

    // ========== EDGE CASES ==========

    @Test
    @DisplayName("distanceTo - Mockito: handles null request body")
    void distanceTo_handlesNullRequestBody() {
        // When
        var result = apiController.distanceTo(null);

        // Then
        assertThat(result.getStatusCode().value()).isEqualTo(400);
        assertThat(result.getBody()).isEqualTo("Invalid request body");
        
        verify(validationService).isValidRequest(null);
        verifyNoMoreInteractions(validationService, distanceService, positionService, regionService);
    }

    @Test
    @DisplayName("nextPosition - Mockito: handles null start position")
    void nextPosition_handlesNullStartPosition() {
        // Given
        NextPositionRequest request = new NextPositionRequest();
        request.setStart(null);
        request.setAngle(0);
        
        when(validationService.isValidRequest(request)).thenReturn(true);

        // When
        var result = apiController.nextPosition(request);

        // Then
        assertThat(result.getStatusCode().value()).isEqualTo(400);
        assertThat(result.getBody()).isEqualTo("Invalid request body");
        
        verify(validationService).isValidRequest(request);
        verifyNoMoreInteractions(validationService, distanceService, positionService, regionService);
    }
}
