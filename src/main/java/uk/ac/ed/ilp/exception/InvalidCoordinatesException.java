package uk.ac.ed.ilp.exception;

/**
 * Custom exception for invalid coordinate values
 * Following Lecture 5 error handling patterns
 */
public class InvalidCoordinatesException extends IllegalArgumentException {
    
    public InvalidCoordinatesException(String message) {
        super(message);
    }
    
    public InvalidCoordinatesException(String message, Throwable cause) {
        super(message, cause);
    }
}
