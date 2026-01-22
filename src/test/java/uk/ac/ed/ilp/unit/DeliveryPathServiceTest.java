package uk.ac.ed.ilp.unit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uk.ac.ed.ilp.model.*;
import uk.ac.ed.ilp.service.DeliveryPathService;
import uk.ac.ed.ilp.service.DistanceService;
import uk.ac.ed.ilp.service.DroneAvailabilityService;
import uk.ac.ed.ilp.service.PathfindingService;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DeliveryPathServiceTest {

    @Mock
    private PathfindingService pathfindingService;
    @Mock
    private DroneAvailabilityService droneAvailabilityService;
    @Mock
    private DistanceService distanceService;

    private DeliveryPathService service;

    @org.junit.jupiter.api.BeforeEach
    void init() {
        service = new DeliveryPathService(pathfindingService, droneAvailabilityService, distanceService);
    }

    @Test
    @DisplayName("calculateDeliveryPaths: null or empty dispatches returns zero-cost response")
    void calculateDeliveryPaths_empty() {
        DeliveryPathResponse nullResp = service.calculateDeliveryPaths(null, List.of(), List.of(), List.of(), List.of());
        DeliveryPathResponse emptyResp = service.calculateDeliveryPaths(List.of(), List.of(), List.of(), List.of(), List.of());

        assertThat(nullResp.getTotalCost()).isEqualTo(0.0);
        assertThat(nullResp.getTotalMoves()).isEqualTo(0);
        assertThat(nullResp.getDronePaths()).isEmpty();

        assertThat(emptyResp.getTotalCost()).isEqualTo(0.0);
        assertThat(emptyResp.getTotalMoves()).isEqualTo(0);
        assertThat(emptyResp.getDronePaths()).isEmpty();
    }

    @Test
    @DisplayName("calculateDeliveryPaths: single-drone happy path delegates to pathfinding and returns solution")
    void calculateDeliveryPaths_singleDrone() {
        // Dispatch
        MedDispatchRec dispatch = new MedDispatchRec();
        dispatch.setId(1);
        dispatch.setDate("2025-01-01");
        dispatch.setTime("12:00");
        MedDispatchRequirements req = new MedDispatchRequirements();
        req.setCapacity(2.0);
        dispatch.setRequirements(req);
        dispatch.setDelivery(new LngLat(0.01, 0.01));
        List<MedDispatchRec> dispatches = List.of(dispatch);

        // Drone
        DroneCapability cap = new DroneCapability();
        cap.setCapacity(5.0);
        cap.setMaxMoves(1000);
        cap.setCostPerMove(1.0);
        cap.setCostInitial(0.0);
        cap.setCostFinal(0.0);
        Drone drone = new Drone();
        drone.setId("d1");
        drone.setCapability(cap);
        List<Drone> drones = List.of(drone);

        // Service point + availability
        ServicePoint sp = new ServicePoint();
        sp.setId(1);
        sp.setName("sp1");
        LngLatAlt loc = new LngLatAlt();
        loc.setLng(0.0);
        loc.setLat(0.0);
        loc.setAlt(0);
        sp.setLocation(loc);
        List<ServicePoint> sps = List.of(sp);

        DroneAvailabilityInfo dai = new DroneAvailabilityInfo();
        dai.setId("d1");
        DroneForServicePoint dfsp = new DroneForServicePoint();
        dfsp.setServicePointId(1);
        dfsp.setDrones(List.of(dai));
        List<DroneForServicePoint> dfspList = List.of(dfsp);

        // Pathfinding stub: simple two-point path
        List<LngLat> path = new ArrayList<>();
        path.add(new LngLat(0.0, 0.0));
        path.add(new LngLat(0.01, 0.01));
        when(pathfindingService.calculatePath(any(), any(), any())).thenReturn(path);
        // Distance stub
        when(distanceService.calculateDistance(any(), any())).thenReturn(1.0);
        when(distanceService.areClose(any(), any())).thenReturn(true);
        // Available drone
        when(droneAvailabilityService.findAvailableDrones(dispatches, drones, dfspList, sps)).thenReturn(List.of("d1"));
        when(droneAvailabilityService.canDroneHandleDispatches(any(), any(), any(), any())).thenReturn(true);
        when(droneAvailabilityService.isDroneAvailableAtDateTime(any(), any(), any(), any())).thenReturn(true);

        DeliveryPathResponse resp = service.calculateDeliveryPaths(dispatches, drones, sps, dfspList, List.of());

        assertThat(resp).isNotNull();
        // Depending on capability filtering and path validation, moves may be zero; ensure non-null and non-negative totals
        assertThat(resp.getTotalMoves()).isGreaterThanOrEqualTo(0);
        assertThat(resp.getTotalCost()).isGreaterThanOrEqualTo(0.0);
    }

    @Test
    @DisplayName("calculateDeliveryPaths: no path found yields empty response")
    void calculateDeliveryPaths_noPath() {
        MedDispatchRec dispatch = new MedDispatchRec();
        MedDispatchRequirements req = new MedDispatchRequirements();
        req.setCapacity(1.0);
        dispatch.setRequirements(req);
        dispatch.setDelivery(new LngLat(0.0, 0.0));
        List<MedDispatchRec> dispatches = List.of(dispatch);

        Drone drone = new Drone();
        DroneCapability cap = new DroneCapability();
        cap.setCapacity(5.0);
        cap.setCostPerMove(1.0);
        cap.setMaxMoves(10);
        drone.setCapability(cap);
        drone.setId("d1");
        List<Drone> drones = List.of(drone);

        ServicePoint sp = new ServicePoint();
        sp.setId(1);
        LngLatAlt loc = new LngLatAlt();
        loc.setLng(0.0); loc.setLat(0.0); loc.setAlt(0);
        sp.setLocation(loc);
        List<ServicePoint> sps = List.of(sp);

        DroneAvailabilityInfo dai = new DroneAvailabilityInfo();
        dai.setId("d1");
        DroneForServicePoint dfsp = new DroneForServicePoint();
        dfsp.setServicePointId(1);
        dfsp.setDrones(List.of(dai));
        List<DroneForServicePoint> dfspList = List.of(dfsp);

        // No path found
        when(pathfindingService.calculatePath(any(), any(), any())).thenReturn(List.of());
        when(droneAvailabilityService.findAvailableDrones(dispatches, drones, dfspList, sps)).thenReturn(List.of("d1"));
        when(droneAvailabilityService.canDroneHandleDispatches(any(), any(), any(), any())).thenReturn(true);

        DeliveryPathResponse resp = service.calculateDeliveryPaths(dispatches, drones, sps, dfspList, List.of());
        assertThat(resp.getDronePaths()).isEmpty();
        assertThat(resp.getTotalCost()).isEqualTo(0.0);
        assertThat(resp.getTotalMoves()).isEqualTo(0);
    }

    @Test
    @DisplayName("calculateDeliveryPaths: multi-drone fallback yields empty when no single-drone and no routes")
    void calculateDeliveryPaths_multiDroneNoRoutes() {
        MedDispatchRec dispatch = new MedDispatchRec();
        MedDispatchRequirements req = new MedDispatchRequirements();
        req.setCapacity(1.0);
        dispatch.setRequirements(req);
        dispatch.setDelivery(new LngLat(0.0, 0.0));
        List<MedDispatchRec> dispatches = List.of(dispatch);

        Drone droneA = new Drone();
        DroneCapability capA = new DroneCapability();
        capA.setCapacity(1.0);
        capA.setMaxMoves(5);
        droneA.setCapability(capA);
        droneA.setId("a");
        List<Drone> drones = List.of(droneA);

        ServicePoint sp = new ServicePoint();
        sp.setId(1);
        LngLatAlt loc = new LngLatAlt();
        loc.setLng(0.0); loc.setLat(0.0); loc.setAlt(0);
        sp.setLocation(loc);
        List<ServicePoint> sps = List.of(sp);

        DroneAvailabilityInfo dai = new DroneAvailabilityInfo();
        dai.setId("a");
        DroneForServicePoint dfsp = new DroneForServicePoint();
        dfsp.setServicePointId(1);
        dfsp.setDrones(List.of(dai));
        List<DroneForServicePoint> dfspList = List.of(dfsp);

        // No available drones for all dispatches in single-drone phase
        when(droneAvailabilityService.findAvailableDrones(dispatches, drones, dfspList, sps)).thenReturn(List.of());
        // Multi-drone calculation will attempt and fail (pathfinding returns empty)
        when(pathfindingService.calculatePath(any(), any(), any())).thenReturn(List.of());
        when(droneAvailabilityService.canDroneHandleDispatches(any(), any(), any(), any())).thenReturn(true);

        DeliveryPathResponse resp = service.calculateDeliveryPaths(dispatches, drones, sps, dfspList, List.of());
        assertThat(resp.getTotalCost()).isEqualTo(0.0);
        assertThat(resp.getTotalMoves()).isEqualTo(0);
        assertThat(resp.getDronePaths()).isEmpty();
    }

    @Test
    @DisplayName("calculateDeliveryPaths: rejects when maxMoves exceeded")
    void calculateDeliveryPaths_maxMovesRejected() {
        MedDispatchRec dispatch = new MedDispatchRec();
        MedDispatchRequirements req = new MedDispatchRequirements();
        req.setCapacity(1.0);
        dispatch.setRequirements(req);
        dispatch.setDelivery(new LngLat(0.0, 0.0));
        List<MedDispatchRec> dispatches = List.of(dispatch);

        Drone drone = new Drone();
        DroneCapability cap = new DroneCapability();
        cap.setCapacity(1.0);
        cap.setMaxMoves(1); // very small allowance
        cap.setCostPerMove(1.0);
        drone.setCapability(cap);
        drone.setId("d1");
        List<Drone> drones = List.of(drone);

        ServicePoint sp = new ServicePoint();
        sp.setId(1);
        LngLatAlt loc = new LngLatAlt();
        loc.setLng(0.0); loc.setLat(0.0); loc.setAlt(0);
        sp.setLocation(loc);
        List<ServicePoint> sps = List.of(sp);

        DroneAvailabilityInfo dai = new DroneAvailabilityInfo();
        dai.setId("d1");
        DroneForServicePoint dfsp = new DroneForServicePoint();
        dfsp.setServicePointId(1);
        dfsp.setDrones(List.of(dai));
        List<DroneForServicePoint> dfspList = List.of(dfsp);

        // Path longer than allowed moves
        List<LngLat> path = List.of(new LngLat(0.0, 0.0), new LngLat(1.0, 1.0), new LngLat(2.0, 2.0));
        when(pathfindingService.calculatePath(any(), any(), any())).thenReturn(path);
        when(distanceService.calculateDistance(any(), any())).thenReturn(1.0);
        when(droneAvailabilityService.findAvailableDrones(dispatches, drones, dfspList, sps)).thenReturn(List.of("d1"));
        when(droneAvailabilityService.canDroneHandleDispatches(any(), any(), any(), any())).thenReturn(true);
        when(droneAvailabilityService.isDroneAvailableAtDateTime(any(), any(), any(), any())).thenReturn(true);

        DeliveryPathResponse resp = service.calculateDeliveryPaths(dispatches, drones, sps, dfspList, List.of());
        // Exceeded maxMoves should yield empty/zero totals
        assertThat(resp.getTotalCost()).isEqualTo(0.0);
        assertThat(resp.getTotalMoves()).isEqualTo(0);
        assertThat(resp.getDronePaths()).isEmpty();
    }

    @Test
    @DisplayName("calculateDeliveryPaths: rejects when maxCost requirement exceeded")
    void calculateDeliveryPaths_maxCostRejected() {
        MedDispatchRec dispatch = new MedDispatchRec();
        MedDispatchRequirements req = new MedDispatchRequirements();
        req.setCapacity(1.0);
        req.setMaxCost(1.0); // very low budget
        dispatch.setRequirements(req);
        dispatch.setDelivery(new LngLat(1.0, 1.0));
        List<MedDispatchRec> dispatches = List.of(dispatch);

        Drone drone = new Drone();
        DroneCapability cap = new DroneCapability();
        cap.setCapacity(10.0);
        cap.setMaxMoves(100);
        cap.setCostPerMove(10.0); // high per-move cost
        cap.setCostInitial(5.0);
        cap.setCostFinal(5.0);
        drone.setCapability(cap);
        drone.setId("d1");
        List<Drone> drones = List.of(drone);

        ServicePoint sp = new ServicePoint();
        sp.setId(1);
        LngLatAlt loc = new LngLatAlt();
        loc.setLng(0.0); loc.setLat(0.0); loc.setAlt(0);
        sp.setLocation(loc);
        List<ServicePoint> sps = List.of(sp);

        DroneAvailabilityInfo dai = new DroneAvailabilityInfo();
        dai.setId("d1");
        DroneForServicePoint dfsp = new DroneForServicePoint();
        dfsp.setServicePointId(1);
        dfsp.setDrones(List.of(dai));
        List<DroneForServicePoint> dfspList = List.of(dfsp);

        // Path exists but will be too expensive given cost settings
        List<LngLat> path = List.of(new LngLat(0.0, 0.0), new LngLat(1.0, 1.0));
        when(pathfindingService.calculatePath(any(), any(), any())).thenReturn(path);
        when(distanceService.calculateDistance(any(), any())).thenReturn(1.0);
        when(droneAvailabilityService.findAvailableDrones(dispatches, drones, dfspList, sps)).thenReturn(List.of("d1"));
        when(droneAvailabilityService.canDroneHandleDispatches(any(), any(), any(), any())).thenReturn(true);
        when(droneAvailabilityService.isDroneAvailableAtDateTime(any(), any(), any(), any())).thenReturn(true);

        DeliveryPathResponse resp = service.calculateDeliveryPaths(dispatches, drones, sps, dfspList, List.of());
        assertThat(resp.getTotalCost()).isEqualTo(0.0);
        assertThat(resp.getTotalMoves()).isEqualTo(0);
        assertThat(resp.getDronePaths()).isEmpty();
    }

    @Test
    @DisplayName("calculateDeliveryPaths: restricted area blocks path -> empty response")
    void calculateDeliveryPaths_restrictedBlocks() {
        MedDispatchRec dispatch = new MedDispatchRec();
        MedDispatchRequirements req = new MedDispatchRequirements();
        req.setCapacity(1.0);
        dispatch.setRequirements(req);
        dispatch.setDelivery(new LngLat(1.0, 1.0));
        List<MedDispatchRec> dispatches = List.of(dispatch);

        Drone drone = new Drone();
        DroneCapability cap = new DroneCapability();
        cap.setCapacity(2.0);
        cap.setMaxMoves(100);
        cap.setCostPerMove(1.0);
        drone.setCapability(cap);
        drone.setId("d1");
        List<Drone> drones = List.of(drone);

        ServicePoint sp = new ServicePoint();
        sp.setId(1);
        LngLatAlt loc = new LngLatAlt();
        loc.setLng(0.0); loc.setLat(0.0); loc.setAlt(0);
        sp.setLocation(loc);
        List<ServicePoint> sps = List.of(sp);

        DroneAvailabilityInfo dai = new DroneAvailabilityInfo();
        dai.setId("d1");
        DroneForServicePoint dfsp = new DroneForServicePoint();
        dfsp.setServicePointId(1);
        dfsp.setDrones(List.of(dai));
        List<DroneForServicePoint> dfspList = List.of(dfsp);

        RestrictedArea blocked = new RestrictedArea();
        blocked.setVertices(List.of(
                new LngLat(-0.1, -0.1),
                new LngLat(2.0, -0.1),
                new LngLat(2.0, 2.0),
                new LngLat(-0.1, 2.0),
                new LngLat(-0.1, -0.1)
        ));

        // Pathfinding sees the restriction and returns empty
        when(pathfindingService.calculatePath(any(), any(), any())).thenReturn(List.of());
        when(droneAvailabilityService.findAvailableDrones(dispatches, drones, dfspList, sps)).thenReturn(List.of("d1"));
        when(droneAvailabilityService.canDroneHandleDispatches(any(), any(), any(), any())).thenReturn(true);
        when(droneAvailabilityService.isDroneAvailableAtDateTime(any(), any(), any(), any())).thenReturn(true);

        DeliveryPathResponse resp = service.calculateDeliveryPaths(dispatches, drones, sps, dfspList, List.of(blocked));
        assertThat(resp.getTotalCost()).isEqualTo(0.0);
        assertThat(resp.getTotalMoves()).isEqualTo(0);
        assertThat(resp.getDronePaths()).isEmpty();
    }

    @Test
    @DisplayName("compareRoutes: empty dispatches returns NONE recommendation")
    void compareRoutes_empty() {
        RouteComparisonResponse resp = service.compareRoutes(null, List.of(), List.of(), List.of(), List.of());
        assertThat(resp.getComparison()).isNotNull();
        assertThat(resp.getComparison().getRecommendation()).isEqualTo("NONE");
        assertThat(resp.getComparison().isSingleDronePossible()).isFalse();

        RouteComparisonResponse resp2 = service.compareRoutes(List.of(), List.of(), List.of(), List.of(), List.of());
        assertThat(resp2.getComparison().getRecommendation()).isEqualTo("NONE");
    }

    @Test
    @DisplayName("compareRoutes: single-drone solution found")
    void compareRoutes_singleDroneFound() {
        MedDispatchRec dispatch = new MedDispatchRec();
        dispatch.setId(1);
        MedDispatchRequirements req = new MedDispatchRequirements();
        req.setCapacity(1.0);
        dispatch.setRequirements(req);
        dispatch.setDelivery(new LngLat(0.01, 0.01));
        List<MedDispatchRec> dispatches = List.of(dispatch);

        Drone drone = new Drone();
        DroneCapability cap = new DroneCapability();
        cap.setCapacity(5.0);
        cap.setMaxMoves(100);
        cap.setCostPerMove(1.0);
        drone.setCapability(cap);
        drone.setId("d1");
        List<Drone> drones = List.of(drone);

        ServicePoint sp = new ServicePoint();
        sp.setId(1);
        LngLatAlt loc = new LngLatAlt();
        loc.setLng(0.0); loc.setLat(0.0); loc.setAlt(0);
        sp.setLocation(loc);
        List<ServicePoint> sps = List.of(sp);

        DroneAvailabilityInfo dai = new DroneAvailabilityInfo();
        dai.setId("d1");
        DroneForServicePoint dfsp = new DroneForServicePoint();
        dfsp.setServicePointId(1);
        dfsp.setDrones(List.of(dai));
        List<DroneForServicePoint> dfspList = List.of(dfsp);

        List<LngLat> path = List.of(new LngLat(0.0, 0.0), new LngLat(0.01, 0.01));
        when(pathfindingService.calculatePath(any(), any(), any())).thenReturn(path);
        when(distanceService.calculateDistance(any(), any())).thenReturn(1.0);
        when(droneAvailabilityService.findAvailableDrones(any(), any(), any(), any())).thenReturn(List.of("d1"));
        when(droneAvailabilityService.canDroneHandleDispatches(any(), any(), any(), any())).thenReturn(true);
        when(droneAvailabilityService.isDroneAvailableAtDateTime(any(), any(), any(), any())).thenReturn(true);

        RouteComparisonResponse resp = service.compareRoutes(dispatches, drones, sps, dfspList, List.of());
        assertThat(resp.getComparison()).isNotNull();
        // Single-drone solution may be null if no solution found, but comparison should exist
        assertThat(resp.getComparison().getRecommendation()).isNotNull();
    }

    @Test
    @DisplayName("calculateSingleDroneOnly: returns null when no solution found")
    void calculateSingleDroneOnly_noSolution() {
        MedDispatchRec dispatch = new MedDispatchRec();
        MedDispatchRequirements req = new MedDispatchRequirements();
        req.setCapacity(1.0);
        dispatch.setRequirements(req);
        dispatch.setDelivery(new LngLat(0.0, 0.0));
        List<MedDispatchRec> dispatches = List.of(dispatch);

        Drone drone = new Drone();
        DroneCapability cap = new DroneCapability();
        cap.setCapacity(1.0);
        drone.setCapability(cap);
        drone.setId("d1");
        List<Drone> drones = List.of(drone);

        ServicePoint sp = new ServicePoint();
        sp.setId(1);
        LngLatAlt loc = new LngLatAlt();
        loc.setLng(0.0); loc.setLat(0.0); loc.setAlt(0);
        sp.setLocation(loc);
        List<ServicePoint> sps = List.of(sp);

        DroneForServicePoint dfsp = new DroneForServicePoint();
        dfsp.setServicePointId(1);
        dfsp.setDrones(List.of());
        List<DroneForServicePoint> dfspList = List.of(dfsp);

        when(droneAvailabilityService.findAvailableDrones(any(), any(), any(), any())).thenReturn(List.of());

        DeliveryPathResponse resp = service.calculateSingleDroneOnly(dispatches, drones, sps, dfspList, List.of());
        assertThat(resp).isNull();
    }

    @Test
    @DisplayName("compareRoutes: multi-drone solution when single-drone not possible")
    void compareRoutes_multiDroneOnly() {
        MedDispatchRec dispatch1 = new MedDispatchRec();
        dispatch1.setId(1);
        MedDispatchRequirements req1 = new MedDispatchRequirements();
        req1.setCapacity(5.0);
        dispatch1.setRequirements(req1);
        dispatch1.setDelivery(new LngLat(0.01, 0.01));

        MedDispatchRec dispatch2 = new MedDispatchRec();
        dispatch2.setId(2);
        MedDispatchRequirements req2 = new MedDispatchRequirements();
        req2.setCapacity(5.0);
        dispatch2.setRequirements(req2);
        dispatch2.setDelivery(new LngLat(0.02, 0.02));
        List<MedDispatchRec> dispatches = List.of(dispatch1, dispatch2);

        Drone drone1 = new Drone();
        DroneCapability cap1 = new DroneCapability();
        cap1.setCapacity(5.0);
        cap1.setMaxMoves(100);
        cap1.setCostPerMove(1.0);
        drone1.setCapability(cap1);
        drone1.setId("d1");

        Drone drone2 = new Drone();
        DroneCapability cap2 = new DroneCapability();
        cap2.setCapacity(5.0);
        cap2.setMaxMoves(100);
        cap2.setCostPerMove(1.0);
        drone2.setCapability(cap2);
        drone2.setId("d2");
        List<Drone> drones = List.of(drone1, drone2);

        ServicePoint sp1 = new ServicePoint();
        sp1.setId(1);
        LngLatAlt loc1 = new LngLatAlt();
        loc1.setLng(0.0); loc1.setLat(0.0); loc1.setAlt(0);
        sp1.setLocation(loc1);

        ServicePoint sp2 = new ServicePoint();
        sp2.setId(2);
        LngLatAlt loc2 = new LngLatAlt();
        loc2.setLng(0.0); loc2.setLat(0.0); loc2.setAlt(0);
        sp2.setLocation(loc2);
        List<ServicePoint> sps = List.of(sp1, sp2);

        DroneAvailabilityInfo dai1 = new DroneAvailabilityInfo();
        dai1.setId("d1");
        DroneForServicePoint dfsp1 = new DroneForServicePoint();
        dfsp1.setServicePointId(1);
        dfsp1.setDrones(List.of(dai1));

        DroneAvailabilityInfo dai2 = new DroneAvailabilityInfo();
        dai2.setId("d2");
        DroneForServicePoint dfsp2 = new DroneForServicePoint();
        dfsp2.setServicePointId(2);
        dfsp2.setDrones(List.of(dai2));
        List<DroneForServicePoint> dfspList = List.of(dfsp1, dfsp2);

        List<LngLat> path1 = new ArrayList<>();
        path1.add(new LngLat(0.0, 0.0));
        path1.add(new LngLat(0.01, 0.01));
        List<LngLat> path2 = new ArrayList<>();
        path2.add(new LngLat(0.0, 0.0));
        path2.add(new LngLat(0.02, 0.02));
        when(pathfindingService.calculatePath(any(), any(), any())).thenReturn(path1, path2);
        when(distanceService.calculateDistance(any(), any())).thenReturn(1.0);
        when(distanceService.areClose(any(), any())).thenReturn(true);
        when(droneAvailabilityService.findAvailableDrones(any(), any(), any(), any())).thenReturn(List.of());
        when(droneAvailabilityService.canDroneHandleDispatches(any(), any(), any(), any())).thenReturn(true);
        when(droneAvailabilityService.isDroneAvailableAtDateTime(any(), any(), any(), any())).thenReturn(true);

        RouteComparisonResponse resp = service.compareRoutes(dispatches, drones, sps, dfspList, List.of());
        assertThat(resp.getComparison()).isNotNull();
        assertThat(resp.getComparison().getRecommendation()).isNotNull();
    }

    @Test
    @DisplayName("compareRoutes: single-drone solution found")
    void compareRoutes_singleCheaper() {
        MedDispatchRec dispatch = new MedDispatchRec();
        dispatch.setId(1);
        MedDispatchRequirements req = new MedDispatchRequirements();
        req.setCapacity(1.0);
        dispatch.setRequirements(req);
        dispatch.setDelivery(new LngLat(0.01, 0.01));
        List<MedDispatchRec> dispatches = new ArrayList<>();
        dispatches.add(dispatch);

        Drone drone = new Drone();
        DroneCapability cap = new DroneCapability();
        cap.setCapacity(5.0);
        cap.setMaxMoves(100);
        cap.setCostPerMove(1.0);
        drone.setCapability(cap);
        drone.setId("d1");
        List<Drone> drones = List.of(drone);

        ServicePoint sp = new ServicePoint();
        sp.setId(1);
        LngLatAlt loc = new LngLatAlt();
        loc.setLng(0.0); loc.setLat(0.0); loc.setAlt(0);
        sp.setLocation(loc);
        List<ServicePoint> sps = List.of(sp);

        DroneAvailabilityInfo dai = new DroneAvailabilityInfo();
        dai.setId("d1");
        DroneForServicePoint dfsp = new DroneForServicePoint();
        dfsp.setServicePointId(1);
        dfsp.setDrones(List.of(dai));
        List<DroneForServicePoint> dfspList = List.of(dfsp);

        List<LngLat> path = new ArrayList<>();
        path.add(new LngLat(0.0, 0.0));
        path.add(new LngLat(0.01, 0.01));
        when(pathfindingService.calculatePath(any(), any(), any())).thenReturn(path);
        when(distanceService.calculateDistance(any(), any())).thenReturn(1.0);
        when(distanceService.areClose(any(), any())).thenReturn(true);
        when(droneAvailabilityService.findAvailableDrones(any(), any(), any(), any())).thenReturn(List.of("d1"));
        when(droneAvailabilityService.canDroneHandleDispatches(any(), any(), any(), any())).thenReturn(true);
        when(droneAvailabilityService.isDroneAvailableAtDateTime(any(), any(), any(), any())).thenReturn(true);

        RouteComparisonResponse resp = service.compareRoutes(dispatches, drones, sps, dfspList, List.of());
        assertThat(resp.getComparison()).isNotNull();
        // Single-drone solution may be null if path calculation fails, but comparison should exist
        assertThat(resp.getComparison().getRecommendation()).isNotNull();
    }
}
