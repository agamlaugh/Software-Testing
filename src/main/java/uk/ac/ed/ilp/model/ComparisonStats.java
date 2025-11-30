package uk.ac.ed.ilp.model;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * Statistics comparing single-drone vs multi-drone solutions
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComparisonStats {
    
    /** Whether a single-drone solution is possible */
    private boolean singleDronePossible;
    
    /** Cost difference as percentage (positive = multi-drone is cheaper) */
    private Double costDifferencePercent;
    
    /** Move difference as percentage (positive = multi-drone has fewer moves) */
    private Double moveDifferencePercent;
    
    /** Recommended solution: "SINGLE_DRONE" or "MULTI_DRONE" */
    private String recommendation;
    
    /** Human-readable explanation for the recommendation */
    private String reason;
    
    /** Single drone cost (if available) */
    private Double singleDroneCost;
    
    /** Multi drone cost */
    private Double multiDroneCost;
    
    /** Single drone moves (if available) */
    private Integer singleDroneMoves;
    
    /** Multi drone moves */
    private Integer multiDroneMoves;
    
    /** Number of drones in multi-drone solution */
    private Integer multiDroneCount;
}

