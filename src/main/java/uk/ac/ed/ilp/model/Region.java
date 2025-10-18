package uk.ac.ed.ilp.model;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Region {
    private String name;
    private List<LngLat> vertices;

    public Region() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<LngLat> getVertices() { return vertices; }
    public void setVertices(List<LngLat> vertices) { this.vertices = vertices; }
}
