package uk.ac.ed.ilp.unit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
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

    @Test
    @DisplayName("POST /api/v1/isCloseTo - WebMvc: boundary 0.00015 is NOT close")
    void isCloseTo_boundary_notClose() throws Exception {
        DistanceRequest request = new DistanceRequest();
        LngLat p1 = new LngLat(0.0, 0.0);
        LngLat p2 = new LngLat(0.00015, 0.0); // distance exactly threshold
        request.setPosition1(p1);
        request.setPosition2(p2);

        when(validationService.isValidRequest(any())).thenReturn(true);
        when(validationService.areValidPositions(any(), any())).thenReturn(true);
        when(distanceService.areClose(any(), any())).thenReturn(false);

        mockMvc.perform(post("/api/v1/isCloseTo")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    // --- Unknown/extra JSON field happy-path tests ---

    @Test
    @DisplayName("POST /api/v1/distanceTo - extra fields are ignored")
    void distanceTo_ignoresExtraFields() throws Exception {
        String requestJson = """
            {
              "position1": {"lng": 0.0, "lat": 0.0, "foo": 1},
              "position2": {"lng": 1.0, "lat": 1.0, "bar": 2},
              "baz": "ignored"
            }
            """;

        when(validationService.isValidRequest(any())).thenReturn(true);
        when(validationService.areValidPositions(any(), any())).thenReturn(true);
        when(distanceService.calculateDistance(any(), any())).thenReturn(1.414);

        mockMvc.perform(post("/api/v1/distanceTo")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(content().string("1.414"));
    }

    @Test
    @DisplayName("POST /api/v1/isCloseTo - extra fields are ignored")
    void isCloseTo_ignoresExtraFields() throws Exception {
        String requestJson = """
            {
              "position1": {"lng": 0.0, "lat": 0.0, "foo": 1},
              "position2": {"lng": 0.0, "lat": 0.0001, "bar": 2},
              "baz": true
            }
            """;

        when(validationService.isValidRequest(any())).thenReturn(true);
        when(validationService.areValidPositions(any(), any())).thenReturn(true);
        when(distanceService.areClose(any(), any())).thenReturn(true);

        mockMvc.perform(post("/api/v1/isCloseTo")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    @DisplayName("POST /api/v1/nextPosition - extra fields are ignored")
    void nextPosition_ignoresExtraFields() throws Exception {
        String requestJson = """
            {
              "start": {"lng": 0.0, "lat": 0.0, "alt": 5, "foo": 7},
              "angle": 90,
              "extra": 42
            }
            """;

        LngLat expected = new LngLat(0.0, 0.00015);
        when(validationService.isValidRequest(any())).thenReturn(true);
        when(validationService.isValidPosition(any())).thenReturn(true);
        when(validationService.isValidAngle(any())).thenReturn(true);
        when(positionService.calculateNextPosition(any(), anyDouble())).thenReturn(expected);

        mockMvc.perform(post("/api/v1/nextPosition")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lng").value(0.0))
                .andExpect(jsonPath("$.lat").value(0.00015));
    }

    @Test
    @DisplayName("POST /api/v1/isInRegion - extra fields are ignored")
    void isInRegion_ignoresExtraFields() throws Exception {
        String requestJson = """
            {
              "position": {"lng": 0.5, "lat": 0.5, "foo": 9},
              "region": {
                "name": "test",
                "vertices": [
                  {"lng": 0.0, "lat": 0.0},
                  {"lng": 1.0, "lat": 0.0},
                  {"lng": 1.0, "lat": 1.0},
                  {"lng": 0.0, "lat": 1.0},
                  {"lng": 0.0, "lat": 0.0}
                ],
                "meta": {"color": "red"}
              },
              "baz": "ignored"
            }
            """;

        when(validationService.isValidRequest(any())).thenReturn(true);
        when(validationService.isValidPosition(any())).thenReturn(true);
        when(validationService.hasValidRegionVertices(any())).thenReturn(true);
        when(validationService.isPolygonClosed(any())).thenReturn(true);
        when(regionService.contains(any(java.util.List.class), any())).thenReturn(true);

        mockMvc.perform(post("/api/v1/isInRegion")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    // --- Region inclusivity (on edge / on vertex) ---

    @Test
    @DisplayName("POST /api/v1/isInRegion - point on edge returns true")
    void isInRegion_pointOnEdge_true() throws Exception {
        String requestJson = """
            {
              "position": {"lng": 0.5, "lat": 0.0},
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
        when(validationService.hasValidRegionVertices(any())).thenReturn(true);
        when(validationService.isPolygonClosed(any())).thenReturn(true);
        when(regionService.contains(any(java.util.List.class), any())).thenReturn(true);

        mockMvc.perform(post("/api/v1/isInRegion")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    @DisplayName("POST /api/v1/isInRegion - point on vertex returns true")
    void isInRegion_pointOnVertex_true() throws Exception {
        String requestJson = """
            {
              "position": {"lng": 0.0, "lat": 0.0},
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
        when(validationService.hasValidRegionVertices(any())).thenReturn(true);
        when(validationService.isPolygonClosed(any())).thenReturn(true);
        when(regionService.contains(any(java.util.List.class), any())).thenReturn(true);

        mockMvc.perform(post("/api/v1/isInRegion")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }
    // Parameterized negative-path coverage for controller validation branches
    static java.util.stream.Stream<NegativeCase> negativeCases() {
        return java.util.stream.Stream.of(
            // distanceTo: invalid request
            NegativeCase.of("/api/v1/distanceTo", "{}", 400, "invalidRequest"),
            // distanceTo: invalid coordinates
            NegativeCase.of("/api/v1/distanceTo", "{\"position1\":{\"lng\":200,\"lat\":0},\"position2\":{\"lng\":0,\"lat\":0}}", 400, "invalidCoords"),
            // isCloseTo: invalid request
            NegativeCase.of("/api/v1/isCloseTo", "{}", 400, "isCloseInvalidRequest"),
            // isCloseTo: invalid coordinates
            NegativeCase.of("/api/v1/isCloseTo", "{\"position1\":{\"lng\":200,\"lat\":0},\"position2\":{\"lng\":0,\"lat\":0}}", 400, "isCloseInvalidCoords"),
            // nextPosition: invalid coords
            NegativeCase.of("/api/v1/nextPosition", "{\"start\":{\"lng\":200,\"lat\":0},\"angle\":0}", 400, "invalidStart"),
            // nextPosition: invalid angle
            NegativeCase.of("/api/v1/nextPosition", "{\"start\":{\"lng\":0,\"lat\":0},\"angle\":91}", 400, "invalidAngle"),
            // nextPosition: missing angle (null)
            NegativeCase.of("/api/v1/nextPosition", "{\"start\":{\"lng\":0,\"lat\":0},\"angle\":null}", 400, "invalidAngleNull"),
            // nextPosition: missing start (null)
            NegativeCase.of("/api/v1/nextPosition", "{\"start\":null,\"angle\":0}", 400, "missingStart"),
            // isInRegion: invalid region vertices
            NegativeCase.of("/api/v1/isInRegion", "{\"position\":{\"lng\":0,\"lat\":0},\"region\":{\"name\":\"x\",\"vertices\":[]}}", 400, "invalidVertices"),
            // isInRegion: null vertices
            NegativeCase.of("/api/v1/isInRegion", "{\"position\":{\"lng\":0,\"lat\":0},\"region\":{\"name\":\"x\",\"vertices\":null}}", 400, "nullVertices"),
            // isInRegion: unclosed polygon
            NegativeCase.of("/api/v1/isInRegion", "{\"position\":{\"lng\":0,\"lat\":0},\"region\":{\"name\":\"x\",\"vertices\":[{\"lng\":0,\"lat\":0},{\"lng\":1,\"lat\":0},{\"lng\":1,\"lat\":1}]}}", 400, "unclosedPolygon")
        );
    }

    @ParameterizedTest(name = "negative case -> {3}")
    @MethodSource("negativeCases")
    @DisplayName("WebMvc: parameterized negative-path validation branches")
    void negative_validation_paths(NegativeCase nc) throws Exception {
        // Default stubs
        when(validationService.isValidRequest(any())).thenReturn(true);
        when(validationService.areValidPositions(any(), any())).thenReturn(true);
        when(validationService.isValidPosition(any())).thenReturn(true);
        when(validationService.isValidAngle(any())).thenReturn(true);
        when(validationService.hasValidRegionVertices(any())).thenReturn(true);
        when(validationService.isPolygonClosed(any())).thenReturn(true);

        // Tailor stubs per scenario
        switch (nc.scenario) {
            case "invalidRequest" -> when(validationService.isValidRequest(any())).thenReturn(false);
            case "invalidCoords" -> when(validationService.areValidPositions(any(), any())).thenReturn(false);
            case "isCloseInvalidRequest" -> when(validationService.isValidRequest(any())).thenReturn(false);
            case "isCloseInvalidCoords" -> when(validationService.areValidPositions(any(), any())).thenReturn(false);
            case "invalidStart" -> when(validationService.isValidPosition(any())).thenReturn(false);
            case "invalidAngle" -> when(validationService.isValidAngle(any())).thenReturn(false);
            case "invalidAngleNull" -> when(validationService.isValidAngle(any())).thenReturn(false);
            case "missingStart" -> when(validationService.isValidRequest(any())).thenReturn(true);
            case "invalidVertices" -> when(validationService.hasValidRegionVertices(any())).thenReturn(false);
            case "nullVertices" -> when(validationService.hasValidRegionVertices(any())).thenReturn(true);
            case "unclosedPolygon" -> when(validationService.isPolygonClosed(any())).thenReturn(false);
            default -> {}
        }

        mockMvc.perform(post(nc.path)
                .contentType(MediaType.APPLICATION_JSON)
                .content(nc.body))
                .andExpect(status().is(nc.expectedStatus));
    }

    private record NegativeCase(String path, String body, int expectedStatus, String scenario) {
        static NegativeCase of(String path, String body, int expectedStatus, String scenario) {
            return new NegativeCase(path, body, expectedStatus, scenario);
        }
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

    @ParameterizedTest(name = "isCloseTo -> expected={2}")
    @CsvSource({
        "0.0,0.0, 0.0001,0.0001, true",
        "0.0,0.0, 1.0,1.0, false"
    })
    @DisplayName("POST /api/v1/isCloseTo - WebMvc: parameterized close/distant cases")
    void isCloseTo_parameterized(double lng1, double lat1, double lng2, double lat2, boolean expected) throws Exception {
        DistanceRequest request = new DistanceRequest();
        LngLat p1 = new LngLat(lng1, lat1);
        LngLat p2 = new LngLat(lng2, lat2);
        request.setPosition1(p1);
        request.setPosition2(p2);

        when(validationService.isValidRequest(any())).thenReturn(true);
        when(validationService.areValidPositions(any(), any())).thenReturn(true);
        when(distanceService.areClose(any(), any())).thenReturn(expected);

        mockMvc.perform(post("/api/v1/isCloseTo")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string(Boolean.toString(expected)));
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
        when(validationService.isValidAngle(any())).thenReturn(true);
        when(positionService.calculateNextPosition(any(), anyDouble())).thenReturn(expectedPosition);

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

    @Test
    @DisplayName("POST /api/v1/nextPosition - WebMvc: returns 400 for missing or invalid angle")
    void nextPosition_returns400_forInvalidAngle() throws Exception {
        String requestJson = """
            {
                "start": {"lng": 0.0, "lat": 0.0},
                "angle": 91
            }
            """;

        when(validationService.isValidRequest(any())).thenReturn(true);
        when(validationService.isValidPosition(any())).thenReturn(true);
        when(validationService.isValidAngle(any())).thenReturn(false);

        mockMvc.perform(post("/api/v1/nextPosition")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid angle"));
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
        when(validationService.hasValidRegionVertices(any())).thenReturn(true);
        when(validationService.isPolygonClosed(any())).thenReturn(true);
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
        when(validationService.hasValidRegionVertices(any())).thenReturn(true);
        when(validationService.isPolygonClosed(any())).thenReturn(false);

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
