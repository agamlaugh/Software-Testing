package uk.ac.ed.ilp.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.ac.ed.ilp.model.requests.DistanceRequest;
import uk.ac.ed.ilp.model.LngLat;
import uk.ac.ed.ilp.model.requests.NextPositionRequest;
import uk.ac.ed.ilp.model.requests.IsInRegionRequest;
import uk.ac.ed.ilp.service.RegionService;

@RestController
@RequestMapping("/api/v1")
public class ApiController {

    private final RegionService regionService = new RegionService();
    private static final String STUDENT_ID = "s2490039";

    @GetMapping(value = "/uid", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> uid() {
        return ResponseEntity.ok(STUDENT_ID);
    }

    @PostMapping(
            value = "/distanceTo",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> distanceTo(@RequestBody(required = false) DistanceRequest req) {
        if (req == null || req.getPosition1() == null || req.getPosition2() == null) {
            return ResponseEntity.badRequest().body("Invalid request body");
        }

        var a = req.getPosition1();
        var b = req.getPosition2();

        if (!a.isValid() || !b.isValid()) {
            return ResponseEntity.badRequest().body("Invalid coordinates");
        }

        double dLat = a.getLat() - b.getLat();
        double dLng = a.getLng() - b.getLng();
        double distance = Math.sqrt(dLat * dLat + dLng * dLng);

        return ResponseEntity.ok(distance);
    }
    @PostMapping(
            value = "/isCloseTo",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> isCloseTo(@RequestBody(required = false) DistanceRequest req) {
        if (req == null || req.getPosition1() == null || req.getPosition2() == null) {
            return ResponseEntity.badRequest().body("Invalid request body");
        }
        var a = req.getPosition1();
        var b = req.getPosition2();
        if (!a.isValid() || !b.isValid()) {
            return ResponseEntity.badRequest().body("Invalid coordinates");
        }

        double dLat = a.getLat() - b.getLat();
        double dLng = a.getLng() - b.getLng();
        double distance = Math.sqrt(dLat * dLat + dLng * dLng);

        boolean result = distance < 0.00015;
        return ResponseEntity.ok(result);
    }
    @PostMapping(
            value = "/nextPosition",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> nextPosition(@RequestBody(required = false) NextPositionRequest req) {
        if (req == null || req.getStart() == null) {
            return ResponseEntity.badRequest().body("Invalid request body");
        }
        LngLat start = req.getStart();
        if (!start.isValid()) {
            return ResponseEntity.badRequest().body("Invalid coordinates");
        }

        final double STEP = 0.00015;
        double radians = Math.toRadians(req.getAngle());
        double newLng = start.getLng() + STEP * Math.cos(radians);
        double newLat = start.getLat() + STEP * Math.sin(radians);

        return ResponseEntity.ok(new LngLat(newLng, newLat));
    }
    @PostMapping(
            value = "/isInRegion",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> isInRegion(@RequestBody(required = false) IsInRegionRequest req) {
        if (req == null || req.getRegionName() == null || req.getPoint() == null) {
            return ResponseEntity.badRequest().body("Invalid request body");
        }
        var name = req.getRegionName();
        var p = req.getPoint();
        if (!p.isValid() || !regionService.hasRegion(name)) {
            return ResponseEntity.badRequest().body("Invalid region or coordinates");
        }
        boolean inside = regionService.contains(name, p);
        return ResponseEntity.ok(inside);
    }
}
