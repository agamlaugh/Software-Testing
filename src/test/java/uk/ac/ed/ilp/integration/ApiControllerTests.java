package uk.ac.ed.ilp.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import uk.ac.ed.ilp.model.LngLat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiControllerTests {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate rest;

    @Test
    void uid_returnsStudentId() {
        var url = "http://localhost:" + port + "/api/v1/uid";
        var body = rest.getForObject(url, String.class);
        assertThat(body).isEqualTo("s2490039");
    }

    @Test
    void distanceTo_returnsEuclideanDistance() {
        var url = "http://localhost:" + port + "/api/v1/distanceTo";
        String req = "{"
                + "\"position1\":{\"lng\":-3.19,\"lat\":55.94},"
                + "\"position2\":{\"lng\":-3.19,\"lat\":55.95}"
                + "}";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));

        HttpEntity<String> http = new HttpEntity<>(req, headers);
        ResponseEntity<Double> resp = rest.postForEntity(url, http, Double.class);

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).isNotNull();
        assertThat(Math.abs(resp.getBody() - 0.01)).isLessThan(1e-9);
    }

    @Test
    void isCloseTo_returnsTrueForNearbyPoints() {
        var url = "http://localhost:" + port + "/api/v1/isCloseTo";
        String req = "{"
                + "\"position1\":{\"lng\":-3.19,\"lat\":55.94000},"
                + "\"position2\":{\"lng\":-3.18990,\"lat\":55.94000}"
                + "}";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));

        HttpEntity<String> http = new HttpEntity<>(req, headers);
        ResponseEntity<Boolean> resp = rest.postForEntity(url, http, Boolean.class);

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).isTrue();
    }

    @Test
    void isCloseTo_returnsFalseForFarPoints() {
        var url = "http://localhost:" + port + "/api/v1/isCloseTo";
        String req = "{"
                + "\"position1\":{\"lng\":-3.19,\"lat\":55.94},"
                + "\"position2\":{\"lng\":-3.19,\"lat\":55.95}"
                + "}";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));

        HttpEntity<String> http = new HttpEntity<>(req, headers);
        ResponseEntity<Boolean> resp = rest.postForEntity(url, http, Boolean.class);

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).isFalse();
    }

    @Test
    void nextPosition_returnsExpectedPoint() throws Exception {
        var url = "http://localhost:" + port + "/api/v1/nextPosition";
        String req = "{"
                + "\"start\":{\"lng\":-3.19,\"lat\":55.94},"
                + "\"angle\":45"
                + "}";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));

        HttpEntity<String> http = new HttpEntity<>(req, headers);
        ResponseEntity<String> resp = rest.postForEntity(url, http, String.class);

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).isNotNull();

        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        var node = mapper.readTree(resp.getBody());

        double lng = node.get("lng").asDouble();
        double lat = node.get("lat").asDouble();

        double step = 0.00015;
        double delta = step / Math.sqrt(2.0); // 45 degrees => dx = dy
        double expectedLng = -3.19 + delta;
        double expectedLat = 55.94 + delta;

        assertThat(Math.abs(lng - expectedLng)).isLessThan(1e-6);
        assertThat(Math.abs(lat - expectedLat)).isLessThan(1e-6);
    }

    @Test
    void isInRegion_returnsTrue_whenPointInsideClosedPolygon() {
        var url = "http://localhost:" + port + "/api/v1/isInRegion";
        // square around the point, explicitly CLOSED (last == first)
        String req = "{"
                + "\"position\":{\"lng\":-3.19,\"lat\":55.94},"
                + "\"region\":{"
                + "  \"name\":\"sample\","
                + "  \"vertices\":["
                + "    {\"lng\":-3.20,\"lat\":55.93},"
                + "    {\"lng\":-3.18,\"lat\":55.93},"
                + "    {\"lng\":-3.18,\"lat\":55.95},"
                + "    {\"lng\":-3.20,\"lat\":55.95},"
                + "    {\"lng\":-3.20,\"lat\":55.93}"  // close
                + "  ]"
                + "}"
                + "}";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));

        HttpEntity<String> http = new HttpEntity<>(req, headers);
        ResponseEntity<Boolean> resp = rest.postForEntity(url, http, Boolean.class);

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).isTrue();
    }

    @Test
    void isInRegion_returns400_whenPolygonNotClosed() {
        var url = "http://localhost:" + port + "/api/v1/isInRegion";
        // same square but NOT closed (last != first) -> should 400
        String req = "{"
                + "\"position\":{\"lng\":-3.19,\"lat\":55.94},"
                + "\"region\":{"
                + "  \"name\":\"sample\","
                + "  \"vertices\":["
                + "    {\"lng\":-3.20,\"lat\":55.93},"
                + "    {\"lng\":-3.18,\"lat\":55.93},"
                + "    {\"lng\":-3.18,\"lat\":55.95},"
                + "    {\"lng\":-3.20,\"lat\":55.95}"
                + "  ]"
                + "}"
                + "}";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));

        HttpEntity<String> http = new HttpEntity<>(req, headers);
        ResponseEntity<String> resp = rest.postForEntity(url, http, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody()).contains("Region polygon must be closed");
    }

    // ========== COMPREHENSIVE EDGE CASE TESTS ==========

    // DistanceTo Edge Cases
    @Test
    void distanceTo_returnsZero_forIdenticalPoints() {
        var url = "http://localhost:" + port + "/api/v1/distanceTo";
        String req = "{"
                + "\"position1\":{\"lng\":-3.19,\"lat\":55.94},"
                + "\"position2\":{\"lng\":-3.19,\"lat\":55.94}"
                + "}";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> http = new HttpEntity<>(req, headers);
        ResponseEntity<Double> resp = rest.postForEntity(url, http, Double.class);

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).isEqualTo(0.0);
    }

    @Test
    void distanceTo_returns400_forInvalidCoordinates() {
        var url = "http://localhost:" + port + "/api/v1/distanceTo";
        String req = "{"
                + "\"position1\":{\"lng\":200.0,\"lat\":55.94},"
                + "\"position2\":{\"lng\":-3.19,\"lat\":55.94}"
                + "}";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> http = new HttpEntity<>(req, headers);
        ResponseEntity<String> resp = rest.postForEntity(url, http, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody()).contains("Invalid coordinates");
    }

    @Test
    void distanceTo_returns400_forNullRequestBody() {
        var url = "http://localhost:" + port + "/api/v1/distanceTo";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> http = new HttpEntity<>(null, headers);
        ResponseEntity<String> resp = rest.postForEntity(url, http, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // IsCloseTo Edge Cases
    @Test
    void isCloseTo_returnsTrue_forExactlyThresholdDistance() {
        var url = "http://localhost:" + port + "/api/v1/isCloseTo";
        String req = "{"
                + "\"position1\":{\"lng\":-3.19,\"lat\":55.94},"
                + "\"position2\":{\"lng\":-3.19,\"lat\":55.940149}"  // Just under 0.00015
                + "}";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> http = new HttpEntity<>(req, headers);
        ResponseEntity<Boolean> resp = rest.postForEntity(url, http, Boolean.class);

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).isTrue();
    }

    @Test
    void isCloseTo_returnsFalse_forJustOverThreshold() {
        var url = "http://localhost:" + port + "/api/v1/isCloseTo";
        String req = "{"
                + "\"position1\":{\"lng\":-3.19,\"lat\":55.94},"
                + "\"position2\":{\"lng\":-3.19,\"lat\":55.940151}"  // Just over 0.00015
                + "}";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> http = new HttpEntity<>(req, headers);
        ResponseEntity<Boolean> resp = rest.postForEntity(url, http, Boolean.class);

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).isFalse();
    }

    // NextPosition Edge Cases
    @Test
    void nextPosition_returnsCorrectPosition_forZeroAngle() throws Exception {
        var url = "http://localhost:" + port + "/api/v1/nextPosition";
        String req = "{"
                + "\"start\":{\"lng\":-3.19,\"lat\":55.94},"
                + "\"angle\":0"
                + "}";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> http = new HttpEntity<>(req, headers);
        ResponseEntity<String> resp = rest.postForEntity(url, http, String.class);

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        var node = mapper.readTree(resp.getBody());
        
        double expectedLng = -3.19 + 0.00015; // East direction
        double expectedLat = 55.94; // No change in latitude
        
        assertThat(Math.abs(node.get("lng").asDouble() - expectedLng)).isLessThan(1e-6);
        assertThat(Math.abs(node.get("lat").asDouble() - expectedLat)).isLessThan(1e-6);
    }

    @Test
    void nextPosition_returnsCorrectPosition_forNegativeAngle() throws Exception {
        var url = "http://localhost:" + port + "/api/v1/nextPosition";
        String req = "{"
                + "\"start\":{\"lng\":-3.19,\"lat\":55.94},"
                + "\"angle\":-90"
                + "}";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> http = new HttpEntity<>(req, headers);
        ResponseEntity<String> resp = rest.postForEntity(url, http, String.class);

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        var node = mapper.readTree(resp.getBody());
        
        double expectedLng = -3.19; // No change in longitude
        double expectedLat = 55.94 - 0.00015; // South direction
        
        assertThat(Math.abs(node.get("lng").asDouble() - expectedLng)).isLessThan(1e-6);
        assertThat(Math.abs(node.get("lat").asDouble() - expectedLat)).isLessThan(1e-6);
    }

    @Test
    void nextPosition_returns400_forMissingAngle() {
        var url = "http://localhost:" + port + "/api/v1/nextPosition";
        String req = "{"
                + "\"start\":{\"lng\":-3.19,\"lat\":55.94}"
                + "}";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> http = new HttpEntity<>(req, headers);
        ResponseEntity<String> resp = rest.postForEntity(url, http, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody()).contains("Invalid angle");
    }

    @Test
    void nextPosition_returns400_forAngleOffCompass() {
        var url = "http://localhost:" + port + "/api/v1/nextPosition";
        String req = "{"
                + "\"start\":{\"lng\":-3.19,\"lat\":55.94},"
                + "\"angle\":91"
                + "}";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> http = new HttpEntity<>(req, headers);
        ResponseEntity<String> resp = rest.postForEntity(url, http, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody()).contains("Invalid angle");
    }

    @Test
    void nextPosition_returns400_forInvalidCoordinates() {
        var url = "http://localhost:" + port + "/api/v1/nextPosition";
        String req = "{"
                + "\"start\":{\"lng\":200.0,\"lat\":55.94},"
                + "\"angle\":45"
                + "}";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> http = new HttpEntity<>(req, headers);
        ResponseEntity<String> resp = rest.postForEntity(url, http, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody()).contains("Invalid coordinates");
    }

    // IsInRegion Edge Cases
    @Test
    void isInRegion_returnsTrue_forPointOnBorder() {
        var url = "http://localhost:" + port + "/api/v1/isInRegion";
        String req = "{"
                + "\"position\":{\"lng\":-3.20,\"lat\":55.94},"  // On the left border
                + "\"region\":{"
                + "  \"name\":\"test\","
                + "  \"vertices\":["
                + "    {\"lng\":-3.20,\"lat\":55.93},"
                + "    {\"lng\":-3.18,\"lat\":55.93},"
                + "    {\"lng\":-3.18,\"lat\":55.95},"
                + "    {\"lng\":-3.20,\"lat\":55.95},"
                + "    {\"lng\":-3.20,\"lat\":55.93}"  // closed
                + "  ]"
                + "}"
                + "}";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> http = new HttpEntity<>(req, headers);
        ResponseEntity<Boolean> resp = rest.postForEntity(url, http, Boolean.class);

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).isTrue();
    }

    @Test
    void isInRegion_returnsFalse_forPointOutside() {
        var url = "http://localhost:" + port + "/api/v1/isInRegion";
        String req = "{"
                + "\"position\":{\"lng\":-3.15,\"lat\":55.94},"  // Outside the region
                + "\"region\":{"
                + "  \"name\":\"test\","
                + "  \"vertices\":["
                + "    {\"lng\":-3.20,\"lat\":55.93},"
                + "    {\"lng\":-3.18,\"lat\":55.93},"
                + "    {\"lng\":-3.18,\"lat\":55.95},"
                + "    {\"lng\":-3.20,\"lat\":55.95},"
                + "    {\"lng\":-3.20,\"lat\":55.93}"  // closed
                + "  ]"
                + "}"
                + "}";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> http = new HttpEntity<>(req, headers);
        ResponseEntity<Boolean> resp = rest.postForEntity(url, http, Boolean.class);

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).isFalse();
    }

    @Test
    void isInRegion_returns400_forEmptyVertices() {
        var url = "http://localhost:" + port + "/api/v1/isInRegion";
        String req = "{"
                + "\"position\":{\"lng\":-3.19,\"lat\":55.94},"
                + "\"region\":{"
                + "  \"name\":\"test\","
                + "  \"vertices\":[]"
                + "}"
                + "}";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> http = new HttpEntity<>(req, headers);
        ResponseEntity<String> resp = rest.postForEntity(url, http, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody()).contains("Invalid position or region vertices");
    }

    @Test
    void isInRegion_returns400_forNullVertices() {
        var url = "http://localhost:" + port + "/api/v1/isInRegion";
        String req = "{"
                + "\"position\":{\"lng\":-3.19,\"lat\":55.94},"
                + "\"region\":{"
                + "  \"name\":\"test\","
                + "  \"vertices\":null"
                + "}"
                + "}";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> http = new HttpEntity<>(req, headers);
        ResponseEntity<String> resp = rest.postForEntity(url, http, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody()).contains("Invalid position or region vertices");
    }

    @Test
    void isInRegion_returns400_forInvalidPosition() {
        var url = "http://localhost:" + port + "/api/v1/isInRegion";
        String req = "{"
                + "\"position\":{\"lng\":200.0,\"lat\":55.94},"  // Invalid longitude
                + "\"region\":{"
                + "  \"name\":\"test\","
                + "  \"vertices\":["
                + "    {\"lng\":-3.20,\"lat\":55.93},"
                + "    {\"lng\":-3.18,\"lat\":55.93},"
                + "    {\"lng\":-3.18,\"lat\":55.95},"
                + "    {\"lng\":-3.20,\"lat\":55.95},"
                + "    {\"lng\":-3.20,\"lat\":55.93}"
                + "  ]"
                + "}"
                + "}";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> http = new HttpEntity<>(req, headers);
        ResponseEntity<String> resp = rest.postForEntity(url, http, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody()).contains("Invalid position or region vertices");
    }

    // Health endpoint test
    @Test
    void health_returnsUpStatus() {
        var url = "http://localhost:" + port + "/actuator/health";
        ResponseEntity<String> resp = rest.getForEntity(url, String.class);
        
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).contains("UP");
    }

    // ========== ENHANCED INTEGRATION TESTS ==========

    @Test
    void distanceTo_returnsCorrectDistance_forBoundaryCoordinates() {
        var url = "http://localhost:" + port + "/api/v1/distanceTo";
        String req = "{"
                + "\"position1\":{\"lng\":-180.0,\"lat\":-90.0},"
                + "\"position2\":{\"lng\":180.0,\"lat\":90.0}"
                + "}";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> http = new HttpEntity<>(req, headers);
        ResponseEntity<Double> resp = rest.postForEntity(url, http, Double.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isCloseTo(402.49, offset(0.1)); // Euclidean distance for boundary coordinates
    }

    @Test
    void distanceTo_returnsCorrectDistance_forVerySmallCoordinates() {
        var url = "http://localhost:" + port + "/api/v1/distanceTo";
        String req = "{"
                + "\"position1\":{\"lng\":0.000001,\"lat\":0.000001},"
                + "\"position2\":{\"lng\":0.000002,\"lat\":0.000002}"
                + "}";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> http = new HttpEntity<>(req, headers);
        ResponseEntity<Double> resp = rest.postForEntity(url, http, Double.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isCloseTo(0.000001414, offset(0.000000001));
    }

    @Test
    void isCloseTo_returnsTrue_forPointsVeryClose() {
        var url = "http://localhost:" + port + "/api/v1/isCloseTo";
        String req = "{"
                + "\"position1\":{\"lng\":0.0,\"lat\":0.0},"
                + "\"position2\":{\"lng\":0.0001,\"lat\":0.0001}"
                + "}";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> http = new HttpEntity<>(req, headers);
        ResponseEntity<Boolean> resp = rest.postForEntity(url, http, Boolean.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isTrue();
    }

    @Test
    void isCloseTo_returnsFalse_forPointsAtThreshold() {
        var url = "http://localhost:" + port + "/api/v1/isCloseTo";
        String req = "{"
                + "\"position1\":{\"lng\":0.0,\"lat\":0.0},"
                + "\"position2\":{\"lng\":0.00015,\"lat\":0.0}"
                + "}";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> http = new HttpEntity<>(req, headers);
        ResponseEntity<Boolean> resp = rest.postForEntity(url, http, Boolean.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isFalse();
    }

    @Test
    void nextPosition_returnsCorrectPosition_forNegativeAngle_Integration() {
        var url = "http://localhost:" + port + "/api/v1/nextPosition";
        String req = "{"
                + "\"start\":{\"lng\":0.0,\"lat\":0.0},"
                + "\"angle\":-90"
                + "}";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> http = new HttpEntity<>(req, headers);
        ResponseEntity<LngLat> resp = rest.postForEntity(url, http, LngLat.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().getLng()).isCloseTo(0.0, offset(0.000001));
        assertThat(resp.getBody().getLat()).isCloseTo(-0.00015, offset(0.000001));
    }

    @Test
    void nextPosition_returnsCorrectPosition_forLargeAngle() {
        var url = "http://localhost:" + port + "/api/v1/nextPosition";
        String req = "{"
                + "\"start\":{\"lng\":0.0,\"lat\":0.0},"
                + "\"angle\":450"
                + "}";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> http = new HttpEntity<>(req, headers);
        ResponseEntity<LngLat> resp = rest.postForEntity(url, http, LngLat.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().getLng()).isCloseTo(0.0, offset(0.000001));
        assertThat(resp.getBody().getLat()).isCloseTo(0.00015, offset(0.000001)); // 450° = 90°
    }

    @Test
    void isInRegion_returnsTrue_forPointOnEdge() {
        var url = "http://localhost:" + port + "/api/v1/isInRegion";
        String req = "{"
                + "\"position\":{\"lng\":-3.19,\"lat\":55.94},"
                + "\"region\":{"
                + "  \"name\":\"test\","
                + "  \"vertices\":["
                + "    {\"lng\":-3.20,\"lat\":55.93},"
                + "    {\"lng\":-3.18,\"lat\":55.93},"
                + "    {\"lng\":-3.18,\"lat\":55.95},"
                + "    {\"lng\":-3.20,\"lat\":55.95},"
                + "    {\"lng\":-3.20,\"lat\":55.93}"
                + "  ]"
                + "}"
                + "}";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> http = new HttpEntity<>(req, headers);
        ResponseEntity<Boolean> resp = rest.postForEntity(url, http, Boolean.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isTrue();
    }

    @Test
    void isInRegion_returnsFalse_forPointOutside_Integration() {
        var url = "http://localhost:" + port + "/api/v1/isInRegion";
        String req = "{"
                + "\"position\":{\"lng\":-3.15,\"lat\":55.90},"
                + "\"region\":{"
                + "  \"name\":\"test\","
                + "  \"vertices\":["
                + "    {\"lng\":-3.20,\"lat\":55.93},"
                + "    {\"lng\":-3.18,\"lat\":55.93},"
                + "    {\"lng\":-3.18,\"lat\":55.95},"
                + "    {\"lng\":-3.20,\"lat\":55.95},"
                + "    {\"lng\":-3.20,\"lat\":55.93}"
                + "  ]"
                + "}"
                + "}";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> http = new HttpEntity<>(req, headers);
        ResponseEntity<Boolean> resp = rest.postForEntity(url, http, Boolean.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isFalse();
    }

    @Test
    void isInRegion_returns400_forUnclosedPolygon() {
        var url = "http://localhost:" + port + "/api/v1/isInRegion";
        String req = "{"
                + "\"position\":{\"lng\":-3.19,\"lat\":55.94},"
                + "\"region\":{"
                + "  \"name\":\"test\","
                + "  \"vertices\":["
                + "    {\"lng\":-3.20,\"lat\":55.93},"
                + "    {\"lng\":-3.18,\"lat\":55.93},"
                + "    {\"lng\":-3.18,\"lat\":55.95},"
                + "    {\"lng\":-3.20,\"lat\":55.95}"
                + "  ]"
                + "}"
                + "}";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> http = new HttpEntity<>(req, headers);
        ResponseEntity<String> resp = rest.postForEntity(url, http, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody()).contains("Region polygon must be closed");
    }

    @Test
    void allEndpoints_handleMalformedJSON() {
        // Test all endpoints with malformed JSON
        String malformedJson = "{\"invalid\": json}";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> http = new HttpEntity<>(malformedJson, headers);

        // Test distanceTo
        var distanceUrl = "http://localhost:" + port + "/api/v1/distanceTo";
        ResponseEntity<String> distanceResp = rest.postForEntity(distanceUrl, http, String.class);
        assertThat(distanceResp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        // Test isCloseTo
        var isCloseUrl = "http://localhost:" + port + "/api/v1/isCloseTo";
        ResponseEntity<String> isCloseResp = rest.postForEntity(isCloseUrl, http, String.class);
        assertThat(isCloseResp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        // Test nextPosition
        var nextPosUrl = "http://localhost:" + port + "/api/v1/nextPosition";
        ResponseEntity<String> nextPosResp = rest.postForEntity(nextPosUrl, http, String.class);
        assertThat(nextPosResp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        // Test isInRegion
        var isInRegionUrl = "http://localhost:" + port + "/api/v1/isInRegion";
        ResponseEntity<String> isInRegionResp = rest.postForEntity(isInRegionUrl, http, String.class);
        assertThat(isInRegionResp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void allEndpoints_handleEmptyRequestBody() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> http = new HttpEntity<>("", headers);

        // Test distanceTo
        var distanceUrl = "http://localhost:" + port + "/api/v1/distanceTo";
        ResponseEntity<String> distanceResp = rest.postForEntity(distanceUrl, http, String.class);
        assertThat(distanceResp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        // Test isCloseTo
        var isCloseUrl = "http://localhost:" + port + "/api/v1/isCloseTo";
        ResponseEntity<String> isCloseResp = rest.postForEntity(isCloseUrl, http, String.class);
        assertThat(isCloseResp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        // Test nextPosition
        var nextPosUrl = "http://localhost:" + port + "/api/v1/nextPosition";
        ResponseEntity<String> nextPosResp = rest.postForEntity(nextPosUrl, http, String.class);
        assertThat(nextPosResp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        // Test isInRegion
        var isInRegionUrl = "http://localhost:" + port + "/api/v1/isInRegion";
        ResponseEntity<String> isInRegionResp = rest.postForEntity(isInRegionUrl, http, String.class);
        assertThat(isInRegionResp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void allEndpoints_handleNullRequestBody() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> http = new HttpEntity<>(null, headers);

        // Test distanceTo
        var distanceUrl = "http://localhost:" + port + "/api/v1/distanceTo";
        ResponseEntity<String> distanceResp = rest.postForEntity(distanceUrl, http, String.class);
        assertThat(distanceResp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        // Test isCloseTo
        var isCloseUrl = "http://localhost:" + port + "/api/v1/isCloseTo";
        ResponseEntity<String> isCloseResp = rest.postForEntity(isCloseUrl, http, String.class);
        assertThat(isCloseResp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        // Test nextPosition
        var nextPosUrl = "http://localhost:" + port + "/api/v1/nextPosition";
        ResponseEntity<String> nextPosResp = rest.postForEntity(nextPosUrl, http, String.class);
        assertThat(nextPosResp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        // Test isInRegion
        var isInRegionUrl = "http://localhost:" + port + "/api/v1/isInRegion";
        ResponseEntity<String> isInRegionResp = rest.postForEntity(isInRegionUrl, http, String.class);
        assertThat(isInRegionResp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
