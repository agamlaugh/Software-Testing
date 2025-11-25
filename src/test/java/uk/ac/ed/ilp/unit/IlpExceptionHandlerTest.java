package uk.ac.ed.ilp.unit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.WebRequest;
import uk.ac.ed.ilp.exception.IlpExceptionHandler;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IlpExceptionHandlerTest {

    private final WebRequest request = mock(WebRequest.class);
    
    IlpExceptionHandlerTest() {
        when(request.getDescription(false)).thenReturn("uri=/api/v1/test");
    }

    @Test
    @DisplayName("Exception handlers map framework errors to expected responses")
    void handlersProduceExpectedResponses() {
        IlpExceptionHandler handler = new IlpExceptionHandler();

        assertResponse(
                handler.handleValidationException(new IllegalArgumentException("invalid payload"), request),
                400,
                "invalid payload"
        );
        @SuppressWarnings("unchecked")
        ResponseEntity<Map<String, Object>> jsonResponse = (ResponseEntity<Map<String, Object>>) handler.handleJsonParseException(
                new org.springframework.http.converter.HttpMessageNotReadableException("boom"), request);
        assertResponse(
                jsonResponse,
                400,
                "Invalid JSON format"
        );
        assertResponse(
                handler.handleNullPointerException(new NullPointerException("missing"), request),
                400,
                "Required field is missing"
        );
        assertResponse(
                handler.handleIllegalStateException(new IllegalStateException("conflict"), request),
                400,
                "conflict"
        );
        @SuppressWarnings("unchecked")
        ResponseEntity<Map<String, Object>> genericResponse = (ResponseEntity<Map<String, Object>>) handler.handleGenericException(
                new RuntimeException("boom"), request);
        assertResponse(
                genericResponse,
                500,
                "An unexpected error occurred"
        );
    }

    private void assertResponse(ResponseEntity<Map<String, Object>> response, int expectedStatus, String expectedMessage) {
        HttpStatus status = HttpStatus.valueOf(response.getStatusCode().value());
        assertThat(response.getStatusCode().value()).isEqualTo(expectedStatus);
        assertThat(response.getBody())
                .containsEntry("status", expectedStatus)
                .containsEntry("message", expectedMessage)
                .containsEntry("error", status.getReasonPhrase())
                .containsEntry("path", "/api/v1/test");
        assertThat(response.getBody().get("timestamp")).isNotNull();
    }
}
