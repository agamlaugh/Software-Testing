package uk.ac.ed.ilp.integration;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiControllerParamTests {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate rest;

    // distanceTo: happy and error cases
    static Stream<TestCase> distanceCases() {
        return Stream.of(
                TestCase.json("/api/v1/distanceTo", 200,
                        "{\n  \"position1\": {\"lng\": -3.19, \"lat\": 55.94},\n  \"position2\": {\"lng\": -3.19, \"lat\": 55.95}\n}"),
                TestCase.json("/api/v1/distanceTo", 400,
                        "{\n  \"position1\": {\"lng\": 200.0, \"lat\": 55.94},\n  \"position2\": {\"lng\": -3.19, \"lat\": 55.94}\n}")
        );
    }

    @ParameterizedTest
    @MethodSource("distanceCases")
    void distanceTo_param(TestCase tc) {
        String url = "http://localhost:" + port + tc.path;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> http = new HttpEntity<>(tc.body, headers);
        ResponseEntity<String> resp = rest.postForEntity(url, http, String.class);
        assertThat(resp.getStatusCode().value()).isEqualTo(tc.expectedStatus);
    }

    // isCloseTo: close vs far
    static Stream<TestCase> closeCases() {
        return Stream.of(
                TestCase.json("/api/v1/isCloseTo", 200,
                        "{\n  \"position1\": {\"lng\": 0.0, \"lat\": 0.0},\n  \"position2\": {\"lng\": 0.0001, \"lat\": 0.0001}\n}"),
                TestCase.json("/api/v1/isCloseTo", 200,
                        "{\n  \"position1\": {\"lng\": 0.0, \"lat\": 0.0},\n  \"position2\": {\"lng\": 0.00015, \"lat\": 0.0}\n}")
        );
    }

    @ParameterizedTest
    @MethodSource("closeCases")
    void isCloseTo_param(TestCase tc) {
        String url = "http://localhost:" + port + tc.path;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> http = new HttpEntity<>(tc.body, headers);
        ResponseEntity<String> resp = rest.postForEntity(url, http, String.class);
        assertThat(resp.getStatusCode().value()).isEqualTo(tc.expectedStatus);
    }

    // nextPosition: valid, invalid angle, invalid coords
    static Stream<TestCase> nextPosCases() {
        return Stream.of(
                TestCase.json("/api/v1/nextPosition", 200,
                        "{\n  \"start\": {\"lng\": -3.19, \"lat\": 55.94},\n  \"angle\": 90\n}"),
                TestCase.json("/api/v1/nextPosition", 400,
                        "{\n  \"start\": {\"lng\": -3.19, \"lat\": 55.94},\n  \"angle\": 91\n}"),
                TestCase.json("/api/v1/nextPosition", 400,
                        "{\n  \"start\": {\"lng\": 200.0, \"lat\": 55.94},\n  \"angle\": 0\n}")
        );
    }

    @ParameterizedTest
    @MethodSource("nextPosCases")
    void nextPosition_param(TestCase tc) {
        String url = "http://localhost:" + port + tc.path;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> http = new HttpEntity<>(tc.body, headers);
        ResponseEntity<String> resp = rest.postForEntity(url, http, String.class);
        assertThat(resp.getStatusCode().value()).isEqualTo(tc.expectedStatus);
    }

    // isInRegion: inside, outside, unclosed, invalid position
    static Stream<TestCase> regionCases() {
        String closed = "{\n  \"name\": \"test\",\n  \"vertices\": [\n    {\"lng\": -3.20, \"lat\": 55.93},\n    {\"lng\": -3.18, \"lat\": 55.93},\n    {\"lng\": -3.18, \"lat\": 55.95},\n    {\"lng\": -3.20, \"lat\": 55.95},\n    {\"lng\": -3.20, \"lat\": 55.93}\n  ]\n}";
        String open = "{\n  \"name\": \"test\",\n  \"vertices\": [\n    {\"lng\": -3.20, \"lat\": 55.93},\n    {\"lng\": -3.18, \"lat\": 55.93},\n    {\"lng\": -3.18, \"lat\": 55.95},\n    {\"lng\": -3.20, \"lat\": 55.95}\n  ]\n}";
        return Stream.of(
                TestCase.json("/api/v1/isInRegion", 200,
                        "{\n  \"position\": {\"lng\": -3.19, \"lat\": 55.94},\n  \"region\": " + closed + "\n}"),
                TestCase.json("/api/v1/isInRegion", 200,
                        "{\n  \"position\": {\"lng\": -3.15, \"lat\": 55.94},\n  \"region\": " + closed + "\n}"),
                TestCase.json("/api/v1/isInRegion", 400,
                        "{\n  \"position\": {\"lng\": -3.19, \"lat\": 55.94},\n  \"region\": " + open + "\n}"),
                TestCase.json("/api/v1/isInRegion", 400,
                        "{\n  \"position\": {\"lng\": 200.0, \"lat\": 55.94},\n  \"region\": " + closed + "\n}")
        );
    }

    @ParameterizedTest
    @MethodSource("regionCases")
    void isInRegion_param(TestCase tc) {
        String url = "http://localhost:" + port + tc.path;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> http = new HttpEntity<>(tc.body, headers);
        ResponseEntity<String> resp = rest.postForEntity(url, http, String.class);
        assertThat(resp.getStatusCode().value()).isEqualTo(tc.expectedStatus);
    }

    // Malformed / empty / null body across endpoints
    static Stream<TestCase> badBodyCases() {
        return Stream.of(
                // malformed JSON
                TestCase.json("/api/v1/distanceTo", 400, "{ invalid json }"),
                TestCase.json("/api/v1/isCloseTo", 400, "{ invalid json }"),
                TestCase.json("/api/v1/nextPosition", 400, "{ invalid json }"),
                TestCase.json("/api/v1/isInRegion", 400, "{ invalid json }"),
                // empty body
                TestCase.json("/api/v1/distanceTo", 400, ""),
                TestCase.json("/api/v1/isCloseTo", 400, ""),
                TestCase.json("/api/v1/nextPosition", 400, ""),
                TestCase.json("/api/v1/isInRegion", 400, ""),
                // null body
                new TestCase("/api/v1/distanceTo", 400, null),
                new TestCase("/api/v1/isCloseTo", 400, null),
                new TestCase("/api/v1/nextPosition", 400, null),
                new TestCase("/api/v1/isInRegion", 400, null)
        );
    }

    @ParameterizedTest
    @MethodSource("badBodyCases")
    void badBodies_param(TestCase tc) {
        String url = "http://localhost:" + port + tc.path;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> http = new HttpEntity<>(tc.body, headers);
        ResponseEntity<String> resp = rest.postForEntity(url, http, String.class);
        assertThat(resp.getStatusCode().value()).isEqualTo(tc.expectedStatus);
    }

    private record TestCase(String path, int expectedStatus, String body) {
        static TestCase json(String path, int expectedStatus, String body) {
            return new TestCase(path, expectedStatus, body);
        }
    }
}


