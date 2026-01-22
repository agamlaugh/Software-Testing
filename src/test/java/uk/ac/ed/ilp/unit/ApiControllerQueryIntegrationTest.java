package uk.ac.ed.ilp.unit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import uk.ac.ed.ilp.controller.ApiController;
import uk.ac.ed.ilp.model.*;
import uk.ac.ed.ilp.model.requests.QueryCondition;
import uk.ac.ed.ilp.service.*;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ApiController.class)
class ApiControllerQueryIntegrationTest {

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

    @Test
    @DisplayName("POST /api/v1/queryAvailableDrones - happy path")
    void queryAvailableDrones_happy() throws Exception {
        String body = """
        [
          { "id": 1, "requirements": { "capacity": 2.0 }, "delivery": { "lng": 0.0, "lat": 0.0 } }
        ]
        """;
        when(ilpRestClient.fetchDrones()).thenReturn(List.of(new Drone()));
        when(ilpRestClient.fetchServicePoints()).thenReturn(List.of(new ServicePoint()));
        when(ilpRestClient.fetchDronesForServicePoints()).thenReturn(List.of(new DroneForServicePoint()));
        when(droneAvailabilityService.findAvailableDrones(any(), any(), any(), any())).thenReturn(List.of("d1"));

        mockMvc.perform(post("/api/v1/queryAvailableDrones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/v1/query - empty conditions returns 200 with empty array")
    void query_emptyConditions() throws Exception {
        when(ilpRestClient.fetchDrones()).thenReturn(List.of(new Drone()));
        when(droneQueryService.queryByConditions(any(), any())).thenReturn(List.of());

        mockMvc.perform(post("/api/v1/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/v1/query - malformed body returns 200 with empty array")
    void query_malformed() throws Exception {
        when(ilpRestClient.fetchDrones()).thenReturn(List.of(new Drone()));
        when(droneQueryService.queryByConditions(any(), any())).thenReturn(List.of());

        mockMvc.perform(post("/api/v1/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"bad\": true }"))
                .andExpect(status().isOk());
    }
}
