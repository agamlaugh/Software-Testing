package uk.ac.ed.ilp.exception;

/**
 * Custom exception for invalid region data
 * Following Lecture 5 error handling patterns
 */
public class InvalidRegionException extends IllegalArgumentException {
    
    public InvalidRegionException(String message) {
        super(message);
    }
    
    public InvalidRegionException(String message, Throwable cause) {
        super(message, cause);
    }
}
