package uk.ac.ed.ilp.model.requests;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

/**
 * Query condition for POST /api/v1/query endpoint
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@NoArgsConstructor
public class QueryCondition {
    private String attribute; // "capacity", "cooling" etc.
    private String operator; 
    private String value; 
}

