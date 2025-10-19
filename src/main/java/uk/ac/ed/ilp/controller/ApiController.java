package uk.ac.ed.ilp.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.ac.ed.ilp.model.requests.DistanceRequest;
import uk.ac.ed.ilp.model.LngLat;
import uk.ac.ed.ilp.model.Region;
import uk.ac.ed.ilp.model.requests.NextPositionRequest;
import uk.ac.ed.ilp.service.RegionService;
import uk.ac.ed.ilp.service.DistanceService;
import uk.ac.ed.ilp.service.PositionService;
import uk.ac.ed.ilp.service.ValidationService;
import uk.ac.ed.ilp.model.requests.IsInRegionSpecRequest;

@RestController
@RequestMapping("/api/v1")
public class ApiController {

    private final RegionService regionService;
    private final DistanceService distanceService;
    private final PositionService positionService;
    private final ValidationService validationService;
    private static final String STUDENT_ID = "s2490039";

    @Autowired
    public ApiController(RegionService regionService, DistanceService distanceService, 
                        PositionService positionService, ValidationService validationService) {
        this.regionService = regionService;
        this.distanceService = distanceService;
        this.positionService = positionService;
        this.validationService = validationService;
    }

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
        if (!validationService.isValidRequest(req) || req.getPosition1() == null || req.getPosition2() == null) {
            return ResponseEntity.badRequest().body("Invalid request body");
        }

        LngLat position1 = req.getPosition1();
        LngLat position2 = req.getPosition2();

        if (!validationService.areValidPositions(position1, position2)) {
            return ResponseEntity.badRequest().body("Invalid coordinates");
        }

        double distance = distanceService.calculateDistance(position1, position2);
        return ResponseEntity.ok(distance);
    }
    @PostMapping(
            value = "/isCloseTo",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> isCloseTo(@RequestBody(required = false) DistanceRequest req) {
        if (!validationService.isValidRequest(req) || req.getPosition1() == null || req.getPosition2() == null) {
            return ResponseEntity.badRequest().body("Invalid request body");
        }
        
        LngLat position1 = req.getPosition1();
        LngLat position2 = req.getPosition2();
        
        if (!validationService.areValidPositions(position1, position2)) {
            return ResponseEntity.badRequest().body("Invalid coordinates");
        }

        boolean result = distanceService.areClose(position1, position2);
        return ResponseEntity.ok(result);
    }
    @PostMapping(
            value = "/nextPosition",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> nextPosition(@RequestBody(required = false) NextPositionRequest req) {
        if (!validationService.isValidRequest(req) || req.getStart() == null) {
            return ResponseEntity.badRequest().body("Invalid request body");
        }
        
        LngLat start = req.getStart();
        if (!validationService.isValidPosition(start)) {
            return ResponseEntity.badRequest().body("Invalid coordinates");
        }

        Double angle = req.getAngle();
        if (!validationService.isValidAngle(angle)) {
            return ResponseEntity.badRequest().body("Invalid angle");
        }

        LngLat nextPosition = positionService.calculateNextPosition(start, angle);
        return ResponseEntity.ok(nextPosition);
    }
    @PostMapping(
            value = "/isInRegion",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> isInRegion(@RequestBody(required = false) IsInRegionSpecRequest req) {
        if (!validationService.isValidRequest(req) || req.getPosition() == null || req.getRegion() == null) {
            return ResponseEntity.badRequest().body("Invalid request body");
        }

        LngLat position = req.getPosition();
        Region region = req.getRegion();

        if (!validationService.isValidPosition(position)) {
            return ResponseEntity.badRequest().body("Invalid position or region vertices");
        }

        if (region.getVertices() == null || region.getVertices().isEmpty()) {
            return ResponseEntity.badRequest().body("Invalid position or region vertices");
        }

        if (!validationService.hasValidRegionVertices(region)) {
            return ResponseEntity.badRequest().body("Invalid position or region vertices");
        }

        if (!validationService.isPolygonClosed(region.getVertices())) {
            return ResponseEntity.badRequest().body("Region polygon must be closed");
        }

        boolean inside = regionService.contains(region.getVertices(), position);
        return ResponseEntity.ok(inside);
    }
}
