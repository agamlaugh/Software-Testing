package uk.ac.ed.ilp.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.ac.ed.ilp.controller.ApiController;
import uk.ac.ed.ilp.model.LngLat;
import uk.ac.ed.ilp.model.Region;
import uk.ac.ed.ilp.model.requests.DistanceRequest;
import uk.ac.ed.ilp.model.requests.IsInRegionSpecRequest;
import uk.ac.ed.ilp.model.requests.NextPositionRequest;
import uk.ac.ed.ilp.service.DistanceService;
import uk.ac.ed.ilp.service.PositionService;
import uk.ac.ed.ilp.service.RegionService;
import uk.ac.ed.ilp.service.ValidationService;
import uk.ac.ed.ilp.service.IlpRestClient;
import uk.ac.ed.ilp.service.DroneQueryService;
import uk.ac.ed.ilp.service.DroneAvailabilityService;
import uk.ac.ed.ilp.service.DeliveryPathService;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApiController Mockito Unit Tests (concise)")
class ApiControllerMockitoTest {

    @Mock private DistanceService distanceService;
    @Mock private PositionService positionService;
    @Mock private ValidationService validationService;
    @Mock private RegionService regionService;
    @Mock private IlpRestClient ilpRestClient;
    @Mock private DroneQueryService droneQueryService;
    @Mock private DroneAvailabilityService droneAvailabilityService;
    @Mock private DeliveryPathService deliveryPathService;

    private ApiController apiController;

    @BeforeEach
    void setup() {
        apiController = new ApiController(regionService, distanceService, positionService, validationService, ilpRestClient, droneQueryService, droneAvailabilityService, deliveryPathService);
    }

    @Test
    @DisplayName("distanceTo: simple happy-path delegates to service")
    void distanceTo_happy() {
        DistanceRequest req = new DistanceRequest();
        LngLat p1 = new LngLat(0.0, 0.0);
        LngLat p2 = new LngLat(1.0, 0.0);
        req.setPosition1(p1); req.setPosition2(p2);

        when(validationService.isValidRequest(req)).thenReturn(true);
        when(validationService.areValidPositions(p1, p2)).thenReturn(true);
        when(distanceService.calculateDistance(p1, p2)).thenReturn(1.0);

        var resp = apiController.distanceTo(req);
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("distanceTo: fail for invalid request body")
    void distanceTo_invalidRequest() {
        DistanceRequest req = new DistanceRequest();
        when(validationService.isValidRequest(req)).thenReturn(false);
        var resp = apiController.distanceTo(req);
        assertThat(resp.getStatusCode().value()).isEqualTo(400);
        assertThat(String.valueOf(resp.getBody())).contains("Invalid request body");
    }

    @Test
    @DisplayName("nextPosition: extreme angle (450° -> 90°) happy-path")
    void nextPosition_extremeAngle() {
        NextPositionRequest req = new NextPositionRequest();
        LngLat start = new LngLat(0.0, 0.0);
        req.setStart(start);
        req.setAngle(450.0);

        LngLat expected = new LngLat(0.0, 0.00015);
        when(validationService.isValidRequest(req)).thenReturn(true);
        when(validationService.isValidPosition(start)).thenReturn(true);
        when(validationService.isValidAngle(450.0)).thenReturn(true);
        when(positionService.calculateNextPosition(start, 450.0)).thenReturn(expected);

        var resp = apiController.nextPosition(req);
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).isEqualTo(expected);
    }

    @Test
    @DisplayName("nextPosition: fail on invalid coordinates")
    void nextPosition_invalidCoords() {
        NextPositionRequest req = new NextPositionRequest();
        LngLat start = new LngLat(200.0, 0.0);
        req.setStart(start);
        req.setAngle(0.0);

        when(validationService.isValidRequest(req)).thenReturn(true);
        when(validationService.isValidPosition(start)).thenReturn(false);

        var resp = apiController.nextPosition(req);
        assertThat(resp.getStatusCode().value()).isEqualTo(400);
        assertThat(String.valueOf(resp.getBody())).contains("Invalid coordinates");
    }

    @Test
    @DisplayName("isInRegion: simple inside closed polygon -> true")
    void isInRegion_insideClosedPolygon() {
        IsInRegionSpecRequest req = new IsInRegionSpecRequest();
        LngLat pos = new LngLat(0.5, 0.5);
        List<LngLat> vertices = Arrays.asList(
                new LngLat(0.0, 0.0), new LngLat(1.0, 0.0),
                new LngLat(1.0, 1.0), new LngLat(0.0, 1.0), new LngLat(0.0, 0.0)
        );
        Region region = new Region();
        region.setVertices(vertices);
        req.setPosition(pos); req.setRegion(region);

        when(validationService.isValidRequest(req)).thenReturn(true);
        when(validationService.isValidPosition(pos)).thenReturn(true);
        when(validationService.hasValidRegionVertices(region)).thenReturn(true);
        when(validationService.isPolygonClosed(vertices)).thenReturn(true);
        when(regionService.contains(vertices, pos)).thenReturn(true);

        var resp = apiController.isInRegion(req);
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).isEqualTo(true);
    }

    @Test
    @DisplayName("isInRegion: fail on unclosed polygon")
    void isInRegion_unclosedPolygon() {
        IsInRegionSpecRequest req = new IsInRegionSpecRequest();
        LngLat pos = new LngLat(0.5, 0.5);
        List<LngLat> vertices = Arrays.asList(
                new LngLat(0.0, 0.0), new LngLat(1.0, 0.0), new LngLat(1.0, 1.0)
        );
        Region region = new Region();
        region.setVertices(vertices);
        req.setPosition(pos); req.setRegion(region);

        when(validationService.isValidRequest(req)).thenReturn(true);
        when(validationService.isValidPosition(pos)).thenReturn(true);
        when(validationService.hasValidRegionVertices(region)).thenReturn(true);
        when(validationService.isPolygonClosed(vertices)).thenReturn(false);

        var resp = apiController.isInRegion(req);
        assertThat(resp.getStatusCode().value()).isEqualTo(400);
        assertThat(String.valueOf(resp.getBody())).contains("Region polygon must be closed");
    }
}


