package uk.ac.ed.ilp.service;

import uk.ac.ed.ilp.model.LngLat;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class RegionService {

    private final Map<String, List<LngLat>> regions = new HashMap<>();

    public RegionService() {
        regions.put("sample", List.of(
            new LngLat(-3.1905, 55.9395),
            new LngLat(-3.1895, 55.9395),
            new LngLat(-3.1895, 55.9405),
            new LngLat(-3.1905, 55.9405)
        ));
    }

    public boolean hasRegion(String name) {
        return regions.containsKey(name);
    }

    public Set<String> regionNames() {
        return regions.keySet();
    }

    public boolean contains(String regionName, LngLat p) {
        var poly = regions.get(regionName);
        if (poly == null || p == null || !p.isValid()) return false;
        return pointInPolygonOrOnBorder(p, poly);
    }

    // --- Geometry helpers ---

    private boolean pointInPolygonOrOnBorder(LngLat p, List<LngLat> poly) {
        // If on border → true
        for (int i = 0, j = poly.size() - 1; i < poly.size(); j = i++) {
            if (pointOnSegment(p, poly.get(j), poly.get(i))) return true;
        }
        // Ray casting
        boolean inside = false;
        for (int i = 0, j = poly.size() - 1; i < poly.size(); j = i++) {
            LngLat a = poly.get(i), b = poly.get(j);
            boolean intersect = ((a.getLat() > p.getLat()) != (b.getLat() > p.getLat())) &&
                (p.getLng() < (b.getLng() - a.getLng()) * (p.getLat() - a.getLat()) /
                               (b.getLat() - a.getLat()) + a.getLng());
            if (intersect) inside = !inside;
        }
        return inside;
    }

    private boolean pointOnSegment(LngLat p, LngLat a, LngLat b) {
        // Check collinearity and bounding box
        double cross = (p.getLat() - a.getLat()) * (b.getLng() - a.getLng())
                     - (p.getLng() - a.getLng()) * (b.getLat() - a.getLat());
        if (Math.abs(cross) > 1e-12) return false;
        double minLng = Math.min(a.getLng(), b.getLng()) - 1e-12;
        double maxLng = Math.max(a.getLng(), b.getLng()) + 1e-12;
        double minLat = Math.min(a.getLat(), b.getLat()) - 1e-12;
        double maxLat = Math.max(a.getLat(), b.getLat()) + 1e-12;
        return p.getLng() >= minLng && p.getLng() <= maxLng &&
               p.getLat() >= minLat && p.getLat() <= maxLat;
    }
    // Overload: check containment for an arbitrary polygon (no name lookup)
    public boolean contains(java.util.List<uk.ac.ed.ilp.model.LngLat> vertices,
                            uk.ac.ed.ilp.model.LngLat p) {
        if (vertices == null || vertices.isEmpty() || p == null || !p.isValid()) return false;
        return pointInPolygonOrOnBorder(p, vertices);
    }
}
