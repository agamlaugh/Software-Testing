package uk.ac.ed.ilp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Restricted Area structure from ILP REST service
 * Limits can be ignored, vertices are rectangular
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@NoArgsConstructor
public class RestrictedArea {
    private String name;
    private Integer id;
    private List<LngLat> vertices; 
}

