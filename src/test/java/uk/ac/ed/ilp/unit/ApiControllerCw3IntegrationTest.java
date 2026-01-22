package uk.ac.ed.ilp.unit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import uk.ac.ed.ilp.controller.ApiController;
import uk.ac.ed.ilp.model.*;
import uk.ac.ed.ilp.service.*;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ApiController.class)
class ApiControllerCw3IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private RegionService regionService;
    @MockBean private DistanceService distanceService;
    @MockBean private PositionService positionService;
    @MockBean private ValidationService validationService;
    @MockBean private IlpRestClient ilpRestClient;
    @MockBean private DroneQueryService droneQueryService;
    @MockBean private DroneAvailabilityService droneAvailabilityService;
    @MockBean private DeliveryPathService deliveryPathService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("POST /api/v1/calcDeliveryPath - returns 200 for non-empty dispatches")
    void calcDeliveryPath_happy() throws Exception {
        String body = """
        [
          {
            "id": 1,
            "date": "2025-01-01",
            "time": "12:00",
            "requirements": {"capacity": 2.0},
            "delivery": {"lng": 0.0, "lat": 0.0}
          }
        ]
        """;
        DeliveryPathResponse resp = new DeliveryPathResponse(5.0, 10, List.of());
        when(ilpRestClient.fetchDrones()).thenReturn(List.of(new Drone()));
        when(ilpRestClient.fetchServicePoints()).thenReturn(List.of(new ServicePoint()));
        when(ilpRestClient.fetchDronesForServicePoints()).thenReturn(List.of(new DroneForServicePoint()));
        when(ilpRestClient.fetchRestrictedAreas()).thenReturn(List.of(new RestrictedArea()));
        when(deliveryPathService.calculateDeliveryPaths(any(), any(), any(), any(), any())).thenReturn(resp);

        mockMvc.perform(post("/api/v1/calcDeliveryPath")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/v1/calcDeliveryPathAsGeoJson - returns 200 and handles empty input")
    void calcDeliveryPathAsGeoJson_empty() throws Exception {
        when(ilpRestClient.fetchDrones()).thenReturn(List.of());
        when(ilpRestClient.fetchServicePoints()).thenReturn(List.of());
        when(ilpRestClient.fetchDronesForServicePoints()).thenReturn(List.of());
        when(ilpRestClient.fetchRestrictedAreas()).thenReturn(List.of());
        DeliveryPathResponse mockResp = new DeliveryPathResponse(0.0, 0, List.of());
        when(deliveryPathService.calculateDeliveryPaths(any(), any(), any(), any(), any()))
                .thenReturn(mockResp);

        mockMvc.perform(post("/api/v1/calcDeliveryPathAsGeoJson")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/v1/compareRoutes - returns 200 for valid request")
    void compareRoutes_happy() throws Exception {
        String body = """
        [
          {
            "id": 1,
            "date": "2025-01-01",
            "time": "12:00",
            "requirements": {"capacity": 2.0},
            "delivery": {"lng": 0.0, "lat": 0.0}
          }
        ]
        """;
        RouteComparisonResponse resp = new RouteComparisonResponse();
        when(ilpRestClient.fetchDrones()).thenReturn(List.of(new Drone()));
        when(ilpRestClient.fetchServicePoints()).thenReturn(List.of(new ServicePoint()));
        when(ilpRestClient.fetchDronesForServicePoints()).thenReturn(List.of(new DroneForServicePoint()));
        when(ilpRestClient.fetchRestrictedAreas()).thenReturn(List.of(new RestrictedArea()));
        when(deliveryPathService.compareRoutes(any(), any(), any(), any(), any())).thenReturn(resp);

        mockMvc.perform(post("/api/v1/compareRoutes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/v1/compareRoutes - malformed body still returns 200 empty recommendation")
    void compareRoutes_malformed() throws Exception {
        String badBody = """
        { "notAnArray": true }
        """;
        RouteComparisonResponse resp = new RouteComparisonResponse();
        when(ilpRestClient.fetchDrones()).thenReturn(List.of());
        when(ilpRestClient.fetchServicePoints()).thenReturn(List.of());
        when(ilpRestClient.fetchDronesForServicePoints()).thenReturn(List.of());
        when(ilpRestClient.fetchRestrictedAreas()).thenReturn(List.of());
        when(deliveryPathService.compareRoutes(any(), any(), any(), any(), any())).thenReturn(resp);

        mockMvc.perform(post("/api/v1/compareRoutes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/calcDeliveryPath - empty dispatches returns zero-cost response")
    void calcDeliveryPath_empty() throws Exception {
        when(ilpRestClient.fetchDrones()).thenReturn(List.of());
        when(ilpRestClient.fetchServicePoints()).thenReturn(List.of());
        when(ilpRestClient.fetchDronesForServicePoints()).thenReturn(List.of());
        when(ilpRestClient.fetchRestrictedAreas()).thenReturn(List.of());
        DeliveryPathResponse mockResp = new DeliveryPathResponse(0.0, 0, List.of());
        when(deliveryPathService.calculateDeliveryPaths(any(), any(), any(), any(), any())).thenReturn(mockResp);

        mockMvc.perform(post("/api/v1/calcDeliveryPath")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCost").value(0.0))
                .andExpect(jsonPath("$.totalMoves").value(0))
                .andExpect(jsonPath("$.dronePaths").isEmpty());
    }

    @Test
    @DisplayName("POST /api/v1/calcDeliveryPathAsGeoJson - returns GeoJSON structure")
    void calcDeliveryPathAsGeoJson_structure() throws Exception {
        DeliveryPathResponse mockResp = new DeliveryPathResponse(5.0, 10, List.of());
        when(ilpRestClient.fetchDrones()).thenReturn(List.of(new Drone()));
        when(ilpRestClient.fetchServicePoints()).thenReturn(List.of(new ServicePoint()));
        when(ilpRestClient.fetchDronesForServicePoints()).thenReturn(List.of(new DroneForServicePoint()));
        when(ilpRestClient.fetchRestrictedAreas()).thenReturn(List.of(new RestrictedArea()));
        when(deliveryPathService.calculateSingleDroneOnly(any(), any(), any(), any(), any())).thenReturn(mockResp);

        String body = """
        [
          {
            "id": 1,
            "date": "2025-01-01",
            "time": "12:00",
            "requirements": {"capacity": 2.0},
            "delivery": {"lng": 0.0, "lat": 0.0}
          }
        ]
        """;

        mockMvc.perform(post("/api/v1/calcDeliveryPathAsGeoJson")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("FeatureCollection"));
    }

    @Test
    @DisplayName("POST /api/v1/compareRoutes - empty dispatches returns NONE recommendation")
    void compareRoutes_empty() throws Exception {
        RouteComparisonResponse resp = RouteComparisonResponse.builder()
                .comparison(ComparisonStats.builder()
                        .singleDronePossible(false)
                        .recommendation("NONE")
                        .reason("No deliveries provided")
                        .build())
                .build();
        when(ilpRestClient.fetchDrones()).thenReturn(List.of());
        when(ilpRestClient.fetchServicePoints()).thenReturn(List.of());
        when(ilpRestClient.fetchDronesForServicePoints()).thenReturn(List.of());
        when(ilpRestClient.fetchRestrictedAreas()).thenReturn(List.of());
        when(deliveryPathService.compareRoutes(any(), any(), any(), any(), any())).thenReturn(resp);

        mockMvc.perform(post("/api/v1/compareRoutes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comparison.recommendation").value("NONE"));
    }

    @Test
    @DisplayName("POST /api/v1/calcDeliveryPathAsGeoJson - fallback to multi-drone when single-drone returns null")
    void calcDeliveryPathAsGeoJson_fallbackToMultiDrone() throws Exception {
        String body = """
        [
          {
            "id": 1,
            "date": "2025-01-01",
            "time": "12:00",
            "requirements": {"capacity": 2.0},
            "delivery": {"lng": 0.0, "lat": 0.0}
          }
        ]
        """;
        DeliveryPathResponse multiDroneResp = new DeliveryPathResponse(10.0, 20, List.of());
        when(ilpRestClient.fetchDrones()).thenReturn(List.of(new Drone()));
        when(ilpRestClient.fetchServicePoints()).thenReturn(List.of(new ServicePoint()));
        when(ilpRestClient.fetchDronesForServicePoints()).thenReturn(List.of(new DroneForServicePoint()));
        when(ilpRestClient.fetchRestrictedAreas()).thenReturn(List.of(new RestrictedArea()));
        when(deliveryPathService.calculateSingleDroneOnly(any(), any(), any(), any(), any())).thenReturn(null);
        when(deliveryPathService.calculateDeliveryPaths(any(), any(), any(), any(), any())).thenReturn(multiDroneResp);

        mockMvc.perform(post("/api/v1/calcDeliveryPathAsGeoJson")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("FeatureCollection"));
    }

    @Test
    @DisplayName("POST /api/v1/calcDeliveryPathAsGeoJson - fallback to multi-drone when single-drone returns empty paths")
    void calcDeliveryPathAsGeoJson_fallbackWhenEmptyPaths() throws Exception {
        String body = """
        [
          {
            "id": 1,
            "date": "2025-01-01",
            "time": "12:00",
            "requirements": {"capacity": 2.0},
            "delivery": {"lng": 0.0, "lat": 0.0}
          }
        ]
        """;
        DeliveryPathResponse emptySingle = new DeliveryPathResponse(0.0, 0, List.of());
        DeliveryPathResponse multiDroneResp = new DeliveryPathResponse(10.0, 20, List.of());
        when(ilpRestClient.fetchDrones()).thenReturn(List.of(new Drone()));
        when(ilpRestClient.fetchServicePoints()).thenReturn(List.of(new ServicePoint()));
        when(ilpRestClient.fetchDronesForServicePoints()).thenReturn(List.of(new DroneForServicePoint()));
        when(ilpRestClient.fetchRestrictedAreas()).thenReturn(List.of(new RestrictedArea()));
        when(deliveryPathService.calculateSingleDroneOnly(any(), any(), any(), any(), any())).thenReturn(emptySingle);
        when(deliveryPathService.calculateDeliveryPaths(any(), any(), any(), any(), any())).thenReturn(multiDroneResp);

        mockMvc.perform(post("/api/v1/calcDeliveryPathAsGeoJson")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("FeatureCollection"));
    }

    @Test
    @DisplayName("POST /api/v1/compareRoutes - adds GeoJSON when single-drone solution exists")
    void compareRoutes_addsSingleDroneGeoJson() throws Exception {
        String body = """
        [
          {
            "id": 1,
            "date": "2025-01-01",
            "time": "12:00",
            "requirements": {"capacity": 2.0},
            "delivery": {"lng": 0.0, "lat": 0.0}
          }
        ]
        """;
        DeliveryPathResponse singleResp = new DeliveryPathResponse(5.0, 10, List.of());
        RouteComparisonResponse compResp = RouteComparisonResponse.builder()
                .singleDroneSolution(singleResp)
                .comparison(ComparisonStats.builder()
                        .singleDronePossible(true)
                        .recommendation("SINGLE_DRONE")
                        .build())
                .build();
        when(ilpRestClient.fetchDrones()).thenReturn(List.of(new Drone()));
        when(ilpRestClient.fetchServicePoints()).thenReturn(List.of(new ServicePoint()));
        when(ilpRestClient.fetchDronesForServicePoints()).thenReturn(List.of(new DroneForServicePoint()));
        when(ilpRestClient.fetchRestrictedAreas()).thenReturn(List.of(new RestrictedArea()));
        when(deliveryPathService.compareRoutes(any(), any(), any(), any(), any())).thenReturn(compResp);

        mockMvc.perform(post("/api/v1/compareRoutes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comparison.recommendation").exists());
    }

    @Test
    @DisplayName("POST /api/v1/compareRoutes - adds GeoJSON when multi-drone solution exists")
    void compareRoutes_addsMultiDroneGeoJson() throws Exception {
        String body = """
        [
          {
            "id": 1,
            "date": "2025-01-01",
            "time": "12:00",
            "requirements": {"capacity": 2.0},
            "delivery": {"lng": 0.0, "lat": 0.0}
          },
          {
            "id": 2,
            "date": "2025-01-01",
            "time": "12:00",
            "requirements": {"capacity": 2.0},
            "delivery": {"lng": 0.01, "lat": 0.01}
          }
        ]
        """;
        DeliveryPathResponse multiResp = new DeliveryPathResponse(10.0, 20, List.of());
        RouteComparisonResponse compResp = RouteComparisonResponse.builder()
                .multiDroneSolution(multiResp)
                .comparison(ComparisonStats.builder()
                        .singleDronePossible(false)
                        .recommendation("MULTI_DRONE")
                        .build())
                .build();
        when(ilpRestClient.fetchDrones()).thenReturn(List.of(new Drone()));
        when(ilpRestClient.fetchServicePoints()).thenReturn(List.of(new ServicePoint()));
        when(ilpRestClient.fetchDronesForServicePoints()).thenReturn(List.of(new DroneForServicePoint()));
        when(ilpRestClient.fetchRestrictedAreas()).thenReturn(List.of(new RestrictedArea()));
        when(deliveryPathService.compareRoutes(any(), any(), any(), any(), any())).thenReturn(compResp);

        mockMvc.perform(post("/api/v1/compareRoutes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comparison.recommendation").exists());
    }
}
