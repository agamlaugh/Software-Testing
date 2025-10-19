package uk.ac.ed.ilp.unit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import uk.ac.ed.ilp.controller.ApiController;
import uk.ac.ed.ilp.model.LngLat;
import uk.ac.ed.ilp.model.requests.DistanceRequest;
import uk.ac.ed.ilp.service.DistanceService;
import uk.ac.ed.ilp.service.PositionService;
import uk.ac.ed.ilp.service.ValidationService;
import uk.ac.ed.ilp.service.RegionService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * WebMvcTest for ApiController
 * Demonstrates controller-only testing as taught in Lecture 4
 * Tests HTTP layer without full Spring context
 */
@WebMvcTest(ApiController.class)
@DisplayName("ApiController WebMvc Tests")
class ApiControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DistanceService distanceService;
    
    @MockBean
    private PositionService positionService;
    
    @MockBean
    private ValidationService validationService;
    
    @MockBean
    private RegionService regionService;

    // ========== UID ENDPOINT TESTS ==========

    @Test
    @DisplayName("GET /api/v1/uid - WebMvc: returns student ID")
    void uid_returnsStudentId() throws Exception {
        mockMvc.perform(get("/api/v1/uid"))
                .andExpect(status().isOk())
                .andExpect(content().string("s2490039"));
    }

    // ========== DISTANCE TO TESTS ==========

    @Test
    @DisplayName("POST /api/v1/distanceTo - WebMvc: returns calculated distance")
    void distanceTo_returnsCalculatedDistance() throws Exception {
        // Given
        DistanceRequest request = new DistanceRequest();
        LngLat p1 = new LngLat(0.0, 0.0);
        LngLat p2 = new LngLat(1.0, 1.0);
        request.setPosition1(p1);
        request.setPosition2(p2);
        
        when(validationService.isValidRequest(any())).thenReturn(true);
        when(validationService.areValidPositions(any(), any())).thenReturn(true);
        when(distanceService.calculateDistance(any(), any())).thenReturn(1.414);

        // When & Then
        mockMvc.perform(post("/api/v1/distanceTo")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("1.414"));
    }

    @Test
    @DisplayName("POST /api/v1/distanceTo - WebMvc: returns 400 for invalid request")
    void distanceTo_returns400_forInvalidRequest() throws Exception {
        // Given
        when(validationService.isValidRequest(any())).thenReturn(false);

        // When & Then
        mockMvc.perform(post("/api/v1/distanceTo")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid request body"));
    }

    @Test
    @DisplayName("POST /api/v1/distanceTo - WebMvc: returns 400 for invalid coordinates")
    void distanceTo_returns400_forInvalidCoordinates() throws Exception {
        // Given
        DistanceRequest request = new DistanceRequest();
        LngLat p1 = new LngLat(200.0, 0.0); // Invalid
        LngLat p2 = new LngLat(1.0, 1.0);
        request.setPosition1(p1);
        request.setPosition2(p2);
        
        when(validationService.isValidRequest(any())).thenReturn(true);
        when(validationService.areValidPositions(any(), any())).thenReturn(false);

        // When & Then
        mockMvc.perform(post("/api/v1/distanceTo")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid coordinates"));
    }

    // ========== IS CLOSE TO TESTS ==========

    @Test
    @DisplayName("POST /api/v1/isCloseTo - WebMvc: returns true for close points")
    void isCloseTo_returnsTrue_forClosePoints() throws Exception {
        // Given
        DistanceRequest request = new DistanceRequest();
        LngLat p1 = new LngLat(0.0, 0.0);
        LngLat p2 = new LngLat(0.0001, 0.0001);
        request.setPosition1(p1);
        request.setPosition2(p2);
        
        when(validationService.isValidRequest(any())).thenReturn(true);
        when(validationService.areValidPositions(any(), any())).thenReturn(true);
        when(distanceService.areClose(any(), any())).thenReturn(true);

        // When & Then
        mockMvc.perform(post("/api/v1/isCloseTo")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    @DisplayName("POST /api/v1/isCloseTo - WebMvc: returns false for distant points")
    void isCloseTo_returnsFalse_forDistantPoints() throws Exception {
        // Given
        DistanceRequest request = new DistanceRequest();
        LngLat p1 = new LngLat(0.0, 0.0);
        LngLat p2 = new LngLat(1.0, 1.0);
        request.setPosition1(p1);
        request.setPosition2(p2);
        
        when(validationService.isValidRequest(any())).thenReturn(true);
        when(validationService.areValidPositions(any(), any())).thenReturn(true);
        when(distanceService.areClose(any(), any())).thenReturn(false);

        // When & Then
        mockMvc.perform(post("/api/v1/isCloseTo")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    // ========== NEXT POSITION TESTS ==========

    @Test
    @DisplayName("POST /api/v1/nextPosition - WebMvc: returns calculated position")
    void nextPosition_returnsCalculatedPosition() throws Exception {
        // Given
        String requestJson = """
            {
                "start": {"lng": 0.0, "lat": 0.0},
                "angle": 90
            }
            """;
        
        LngLat expectedPosition = new LngLat(0.0, 0.00015);
        
        when(validationService.isValidRequest(any())).thenReturn(true);
        when(validationService.isValidPosition(any())).thenReturn(true);
        when(positionService.calculateNextPosition(any(), any(Integer.class))).thenReturn(expectedPosition);

        // When & Then
        mockMvc.perform(post("/api/v1/nextPosition")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lng").value(0.0))
                .andExpect(jsonPath("$.lat").value(0.00015));
    }

    @Test
    @DisplayName("POST /api/v1/nextPosition - WebMvc: returns 400 for invalid position")
    void nextPosition_returns400_forInvalidPosition() throws Exception {
        // Given
        String requestJson = """
            {
                "start": {"lng": 200.0, "lat": 0.0},
                "angle": 90
            }
            """;
        
        when(validationService.isValidRequest(any())).thenReturn(true);
        when(validationService.isValidPosition(any())).thenReturn(false);

        // When & Then
        mockMvc.perform(post("/api/v1/nextPosition")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid coordinates"));
    }

    // ========== IS IN REGION TESTS ==========

    @Test
    @DisplayName("POST /api/v1/isInRegion - WebMvc: returns true for point inside region")
    void isInRegion_returnsTrue_forPointInside() throws Exception {
        // Given
        String requestJson = """
            {
                "position": {"lng": 0.5, "lat": 0.5},
                "region": {
                    "name": "test",
                    "vertices": [
                        {"lng": 0.0, "lat": 0.0},
                        {"lng": 1.0, "lat": 0.0},
                        {"lng": 1.0, "lat": 1.0},
                        {"lng": 0.0, "lat": 1.0},
                        {"lng": 0.0, "lat": 0.0}
                    ]
                }
            }
            """;
        
        when(validationService.isValidRequest(any())).thenReturn(true);
        when(validationService.isValidPosition(any())).thenReturn(true);
        when(validationService.isValidRegion(any())).thenReturn(true);
        when(regionService.contains(any(java.util.List.class), any())).thenReturn(true);

        // When & Then
        mockMvc.perform(post("/api/v1/isInRegion")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    @DisplayName("POST /api/v1/isInRegion - WebMvc: returns 400 for unclosed polygon")
    void isInRegion_returns400_forUnclosedPolygon() throws Exception {
        // Given
        String requestJson = """
            {
                "position": {"lng": 0.5, "lat": 0.5},
                "region": {
                    "name": "test",
                    "vertices": [
                        {"lng": 0.0, "lat": 0.0},
                        {"lng": 1.0, "lat": 0.0},
                        {"lng": 1.0, "lat": 1.0},
                        {"lng": 0.0, "lat": 1.0}
                    ]
                }
            }
            """;
        
        when(validationService.isValidRequest(any())).thenReturn(true);
        when(validationService.isValidPosition(any())).thenReturn(true);
        when(validationService.isValidRegion(any())).thenReturn(false);

        // When & Then
        mockMvc.perform(post("/api/v1/isInRegion")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Region polygon must be closed"));
    }

    // ========== ERROR HANDLING TESTS ==========

    @Test
    @DisplayName("POST /api/v1/distanceTo - WebMvc: handles malformed JSON")
    void distanceTo_handlesMalformedJson() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/distanceTo")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ invalid json }"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/distanceTo - WebMvc: handles empty request body")
    void distanceTo_handlesEmptyRequestBody() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/distanceTo")
                .contentType(MediaType.APPLICATION_JSON)
                .content(""))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/nonexistent - WebMvc: returns 500 for unknown endpoint (handled by exception handler)")
    void nonexistentEndpoint_returns500() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/nonexistent"))
                .andExpect(status().isInternalServerError());
    }
}
