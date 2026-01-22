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
import uk.ac.ed.ilp.model.Drone;
import uk.ac.ed.ilp.model.DroneForServicePoint;
import uk.ac.ed.ilp.model.MedDispatchRec;
import uk.ac.ed.ilp.model.RestrictedArea;
import uk.ac.ed.ilp.model.ServicePoint;
import uk.ac.ed.ilp.model.DeliveryPathResponse;

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

    // ========== CW3 DELIVERY PATH ENDPOINT TESTS ==========

    @Test
    @DisplayName("calcDeliveryPath: null or empty dispatches returns empty response")
    void calcDeliveryPath_emptyDispatches() {
        var respNull = apiController.calcDeliveryPath(null);
        assertThat(respNull.getStatusCode().value()).isEqualTo(200);
        assertThat(respNull.getBody().getTotalCost()).isEqualTo(0.0);
        assertThat(respNull.getBody().getDronePaths()).isEmpty();

        var respEmpty = apiController.calcDeliveryPath(List.of());
        assertThat(respEmpty.getStatusCode().value()).isEqualTo(200);
        assertThat(respEmpty.getBody().getTotalCost()).isEqualTo(0.0);
        assertThat(respEmpty.getBody().getDronePaths()).isEmpty();
    }

    @Test
    @DisplayName("calcDeliveryPath: delegates to service with fetched data")
    void calcDeliveryPath_delegatesToService() {
        List<MedDispatchRec> dispatches = List.of(new MedDispatchRec());
        List<Drone> drones = List.of(new Drone());
        List<ServicePoint> servicePoints = List.of(new ServicePoint());
        List<DroneForServicePoint> dfsp = List.of(new DroneForServicePoint());
        List<RestrictedArea> restricted = List.of(new RestrictedArea());

        when(ilpRestClient.fetchDrones()).thenReturn(drones);
        when(ilpRestClient.fetchServicePoints()).thenReturn(servicePoints);
        when(ilpRestClient.fetchDronesForServicePoints()).thenReturn(dfsp);
        when(ilpRestClient.fetchRestrictedAreas()).thenReturn(restricted);
        DeliveryPathResponse expected = new DeliveryPathResponse(10.0, 5, List.of());
        when(deliveryPathService.calculateDeliveryPaths(dispatches, drones, servicePoints, dfsp, restricted))
                .thenReturn(expected);

        var resp = apiController.calcDeliveryPath(dispatches);
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).isSameAs(expected);
    }

    @Test
    @DisplayName("distanceTo: null position1 returns 400")
    void distanceTo_nullPosition1() {
        DistanceRequest req = new DistanceRequest();
        req.setPosition1(null);
        req.setPosition2(new LngLat(1.0, 1.0));
        when(validationService.isValidRequest(req)).thenReturn(true);
        
        var resp = apiController.distanceTo(req);
        assertThat(resp.getStatusCode().value()).isEqualTo(400);
    }

    @Test
    @DisplayName("distanceTo: null position2 returns 400")
    void distanceTo_nullPosition2() {
        DistanceRequest req = new DistanceRequest();
        req.setPosition1(new LngLat(0.0, 0.0));
        req.setPosition2(null);
        when(validationService.isValidRequest(req)).thenReturn(true);
        
        var resp = apiController.distanceTo(req);
        assertThat(resp.getStatusCode().value()).isEqualTo(400);
    }

    @Test
    @DisplayName("isCloseTo: happy path delegates to service")
    void isCloseTo_happy() {
        DistanceRequest req = new DistanceRequest();
        LngLat p1 = new LngLat(0.0, 0.0);
        LngLat p2 = new LngLat(0.0001, 0.0001);
        req.setPosition1(p1);
        req.setPosition2(p2);
        
        when(validationService.isValidRequest(req)).thenReturn(true);
        when(validationService.areValidPositions(p1, p2)).thenReturn(true);
        when(distanceService.areClose(p1, p2)).thenReturn(true);
        
        var resp = apiController.isCloseTo(req);
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).isEqualTo(true);
    }

    @Test
    @DisplayName("isCloseTo: null position1 returns 400")
    void isCloseTo_nullPosition1() {
        DistanceRequest req = new DistanceRequest();
        req.setPosition1(null);
        req.setPosition2(new LngLat(1.0, 1.0));
        when(validationService.isValidRequest(req)).thenReturn(true);
        
        var resp = apiController.isCloseTo(req);
        assertThat(resp.getStatusCode().value()).isEqualTo(400);
    }

    @Test
    @DisplayName("nextPosition: null start returns 400")
    void nextPosition_nullStart() {
        NextPositionRequest req = new NextPositionRequest();
        req.setStart(null);
        req.setAngle(45.0);
        when(validationService.isValidRequest(req)).thenReturn(true);
        
        var resp = apiController.nextPosition(req);
        assertThat(resp.getStatusCode().value()).isEqualTo(400);
    }

    @Test
    @DisplayName("isInRegion: null position returns 400")
    void isInRegion_nullPosition() {
        IsInRegionSpecRequest req = new IsInRegionSpecRequest();
        req.setPosition(null);
        Region region = new Region();
        region.setVertices(List.of(new LngLat(0.0, 0.0)));
        req.setRegion(region);
        when(validationService.isValidRequest(req)).thenReturn(true);
        
        var resp = apiController.isInRegion(req);
        assertThat(resp.getStatusCode().value()).isEqualTo(400);
    }

    @Test
    @DisplayName("isInRegion: null region returns 400")
    void isInRegion_nullRegion() {
        IsInRegionSpecRequest req = new IsInRegionSpecRequest();
        req.setPosition(new LngLat(0.5, 0.5));
        req.setRegion(null);
        when(validationService.isValidRequest(req)).thenReturn(true);
        
        var resp = apiController.isInRegion(req);
        assertThat(resp.getStatusCode().value()).isEqualTo(400);
    }

    @Test
    @DisplayName("isInRegion: empty vertices returns 400")
    void isInRegion_emptyVertices() {
        IsInRegionSpecRequest req = new IsInRegionSpecRequest();
        req.setPosition(new LngLat(0.5, 0.5));
        Region region = new Region();
        region.setVertices(List.of());
        req.setRegion(region);
        when(validationService.isValidRequest(req)).thenReturn(true);
        when(validationService.isValidPosition(any())).thenReturn(true);
        
        var resp = apiController.isInRegion(req);
        assertThat(resp.getStatusCode().value()).isEqualTo(400);
    }

    @Test
    @DisplayName("isInRegion: invalid vertices returns 400")
    void isInRegion_invalidVertices() {
        IsInRegionSpecRequest req = new IsInRegionSpecRequest();
        LngLat pos = new LngLat(0.5, 0.5);
        Region region = new Region();
        region.setVertices(List.of(new LngLat(0.0, 0.0)));
        req.setPosition(pos);
        req.setRegion(region);
        
        when(validationService.isValidRequest(req)).thenReturn(true);
        when(validationService.isValidPosition(pos)).thenReturn(true);
        when(validationService.hasValidRegionVertices(region)).thenReturn(false);
        
        var resp = apiController.isInRegion(req);
        assertThat(resp.getStatusCode().value()).isEqualTo(400);
    }

    @Test
    @DisplayName("droneDetails: returns drone when found")
    void droneDetails_found() {
        Drone drone = new Drone();
        drone.setId("d1");
        when(ilpRestClient.fetchDrones()).thenReturn(List.of(drone));
        
        var resp = apiController.droneDetails("d1");
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).isSameAs(drone);
    }

    @Test
    @DisplayName("droneDetails: returns 404 when not found")
    void droneDetails_notFound() {
        when(ilpRestClient.fetchDrones()).thenReturn(List.of());
        
        var resp = apiController.droneDetails("nonexistent");
        assertThat(resp.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    @DisplayName("queryAsPath: delegates to service")
    void queryAsPath_delegates() {
        Drone drone = new Drone();
        drone.setId("d1");
        drone.setName("test");
        when(ilpRestClient.fetchDrones()).thenReturn(List.of(drone));
        when(droneQueryService.queryByAttribute(any(), eq("name"), eq("test"))).thenReturn(List.of("d1"));
        
        var resp = apiController.queryAsPath("name", "test");
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).contains("d1");
    }

    @Test
    @DisplayName("query: null conditions returns empty list")
    void query_nullConditions() {
        var resp = apiController.query(null);
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).isEmpty();
    }

    @Test
    @DisplayName("query: empty conditions returns empty list")
    void query_emptyConditions() {
        var resp = apiController.query(List.of());
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).isEmpty();
    }

    @Test
    @DisplayName("queryAvailableDrones: null dispatches returns empty list")
    void queryAvailableDrones_null() {
        var resp = apiController.queryAvailableDrones(null);
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).isEmpty();
    }

    @Test
    @DisplayName("queryAvailableDrones: empty dispatches returns empty list")
    void queryAvailableDrones_empty() {
        var resp = apiController.queryAvailableDrones(List.of());
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).isEmpty();
    }

    @Test
    @DisplayName("dronesWithCooling: filters by cooling state")
    void dronesWithCooling_filters() {
        Drone drone1 = new Drone();
        drone1.setId("d1");
        uk.ac.ed.ilp.model.DroneCapability cap1 = new uk.ac.ed.ilp.model.DroneCapability();
        cap1.setCooling(true);
        drone1.setCapability(cap1);
        
        Drone drone2 = new Drone();
        drone2.setId("d2");
        uk.ac.ed.ilp.model.DroneCapability cap2 = new uk.ac.ed.ilp.model.DroneCapability();
        cap2.setCooling(false);
        drone2.setCapability(cap2);
        
        when(ilpRestClient.fetchDrones()).thenReturn(List.of(drone1, drone2));
        
        var resp = apiController.dronesWithCooling(true);
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).containsExactly("d1");
        
        var resp2 = apiController.dronesWithCooling(false);
        assertThat(resp2.getStatusCode().value()).isEqualTo(200);
        assertThat(resp2.getBody()).containsExactly("d2");
    }
}


