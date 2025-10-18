package uk.ac.ed.ilp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;

import static org.assertj.core.api.Assertions.assertThat;

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
        boolean valid = node.get("valid").asBoolean();

        double step = 0.00015;
        double delta = step / Math.sqrt(2.0); // 45 degrees => dx = dy
        double expectedLng = -3.19 + delta;
        double expectedLat = 55.94 + delta;

        assertThat(Math.abs(lng - expectedLng)).isLessThan(1e-6);
        assertThat(Math.abs(lat - expectedLat)).isLessThan(1e-6);
        assertThat(valid).isTrue();
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
}
