package uk.ac.ed.ilp.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.ac.ed.ilp.model.requests.DistanceRequest;
import uk.ac.ed.ilp.model.LngLat;

@RestController
@RequestMapping("/api/v1")
public class ApiController {

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
}
