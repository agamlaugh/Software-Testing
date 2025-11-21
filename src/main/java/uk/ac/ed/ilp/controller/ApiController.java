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
import uk.ac.ed.ilp.service.IlpRestClient;
import uk.ac.ed.ilp.service.DroneQueryService;
import uk.ac.ed.ilp.service.DroneAvailabilityService;
import uk.ac.ed.ilp.model.requests.IsInRegionSpecRequest;
import uk.ac.ed.ilp.model.requests.QueryCondition;
import uk.ac.ed.ilp.model.Drone;
import uk.ac.ed.ilp.model.MedDispatchRec;
import uk.ac.ed.ilp.model.DroneForServicePoint;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1")
public class ApiController {

    private final RegionService regionService;
    private final DistanceService distanceService;
    private final PositionService positionService;
    private final ValidationService validationService;
    private final IlpRestClient ilpRestClient;
    private final DroneQueryService droneQueryService;
    private final DroneAvailabilityService droneAvailabilityService;
    private static final String STUDENT_ID = "s2490039";

    @Autowired
    public ApiController(RegionService regionService, DistanceService distanceService, 
                        PositionService positionService, ValidationService validationService,
                        IlpRestClient ilpRestClient, DroneQueryService droneQueryService,
                        DroneAvailabilityService droneAvailabilityService) {
        this.regionService = regionService;
        this.distanceService = distanceService;
        this.positionService = positionService;
        this.validationService = validationService;
        this.ilpRestClient = ilpRestClient;
        this.droneQueryService = droneQueryService;
        this.droneAvailabilityService = droneAvailabilityService;
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
    
    @GetMapping(value = "/dronesWithCooling/{state}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<String>> dronesWithCooling(@PathVariable Boolean state) {
        List<Drone> drones = ilpRestClient.fetchDrones();
                List<String> droneIds = drones.stream()
                .filter(drone -> drone != null && drone.getCapability() != null)
                .filter(drone -> {
                    Boolean cooling = drone.getCapability().getCooling();
                    boolean hasCooling = Boolean.TRUE.equals(cooling);
                    return hasCooling == state;
                })
                .map(Drone::getId)
                .filter(id -> id != null)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(droneIds);
    }

    @GetMapping(value = "/droneDetails/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Drone> droneDetails(@PathVariable String id) {
        List<Drone> drones = ilpRestClient.fetchDrones();
        
        // Find drone with matching ID
        Drone drone = drones.stream()
                .filter(d -> d != null && id.equals(d.getId()))
                .findFirst()
                .orElse(null);
        
        if (drone == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(drone);
    }

    /**
     * Returns array of drone IDs where attribute equals value
     * Single attribute query, always uses = operator
     */
    @GetMapping(value = "/queryAsPath/{attribute}/{value}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<String>> queryAsPath(
            @PathVariable String attribute,
            @PathVariable String value) {
        
        // Fetch fresh data from ILP REST service
        List<Drone> drones = ilpRestClient.fetchDrones();
        
        // Delegate to service for query logic
        List<String> droneIds = droneQueryService.queryByAttribute(drones, attribute, value);
        
        return ResponseEntity.ok(droneIds);
    }

    /**
     * Returns array of drone IDs matching multiple conditions with operators
     * All conditions joined by AND logic
     * Supports operators: =, !=, <, > for numerical attributes; only = for boolean/string
     */
    @PostMapping(
            value = "/query",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<List<String>> query(@RequestBody(required = false) List<QueryCondition> conditions) {
        // Validate request
        if (conditions == null || conditions.isEmpty()) {
            return ResponseEntity.badRequest().body(List.of());
        }
        
        // Fetch fresh data from ILP REST service
        List<Drone> drones = ilpRestClient.fetchDrones();
        
        // Delegate to service for query logic (AND logic - all conditions must match)
        List<String> droneIds = droneQueryService.queryByConditions(drones, conditions);
        
        return ResponseEntity.ok(droneIds);
    }

    /**
     * Returns array of drone IDs that can handle all dispatches
     * All dispatches joined by AND logic - one drone must handle all
     * Considers: capability requirements, date/time availability, service point availability
     */
    @PostMapping(
            value = "/queryAvailableDrones",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<List<String>> queryAvailableDrones(
            @RequestBody(required = false) List<MedDispatchRec> dispatches) {
        
        // Validate request
        if (dispatches == null || dispatches.isEmpty()) {
            return ResponseEntity.ok(List.of()); // Empty array per spec
        }
        
        // Fetch fresh data from ILP REST service
        List<Drone> drones = ilpRestClient.fetchDrones();
        List<DroneForServicePoint> dronesForServicePoints = ilpRestClient.fetchDronesForServicePoints();
        
        // Find drones that can handle all dispatches
        List<String> droneIds = droneAvailabilityService.findAvailableDrones(
                dispatches, drones, dronesForServicePoints);
        
        return ResponseEntity.ok(droneIds);
    }
}
