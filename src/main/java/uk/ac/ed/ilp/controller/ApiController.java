package uk.ac.ed.ilp.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.ac.ed.ilp.model.requests.DistanceRequest;
import uk.ac.ed.ilp.model.LngLat;
import uk.ac.ed.ilp.model.LngLatAlt;
import uk.ac.ed.ilp.model.Region;
import uk.ac.ed.ilp.model.requests.NextPositionRequest;
import uk.ac.ed.ilp.service.RegionService;
import uk.ac.ed.ilp.service.DistanceService;
import uk.ac.ed.ilp.service.PositionService;
import uk.ac.ed.ilp.service.ValidationService;
import uk.ac.ed.ilp.service.IlpRestClient;
import uk.ac.ed.ilp.service.DroneQueryService;
import uk.ac.ed.ilp.service.DroneAvailabilityService;
import uk.ac.ed.ilp.service.DeliveryPathService;
import uk.ac.ed.ilp.model.requests.IsInRegionSpecRequest;
import uk.ac.ed.ilp.model.requests.QueryCondition;
import uk.ac.ed.ilp.model.Drone;
import uk.ac.ed.ilp.model.MedDispatchRec;
import uk.ac.ed.ilp.model.DroneForServicePoint;
import uk.ac.ed.ilp.model.DeliveryPathResponse;
import uk.ac.ed.ilp.model.ServicePoint;
import uk.ac.ed.ilp.model.RestrictedArea;
import uk.ac.ed.ilp.model.GeoJsonFeatureCollection;
import uk.ac.ed.ilp.model.GeoJsonFeature;
import uk.ac.ed.ilp.model.GeoJsonGeometry;
import uk.ac.ed.ilp.model.GeoJsonProperties;
import uk.ac.ed.ilp.model.DronePath;
import uk.ac.ed.ilp.model.DeliveryPath;

import java.util.List;
import java.util.ArrayList;
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
    private final DeliveryPathService deliveryPathService;
    private static final String STUDENT_ID = "s2490039";

    @Autowired
    public ApiController(RegionService regionService, DistanceService distanceService, 
                        PositionService positionService, ValidationService validationService,
                        IlpRestClient ilpRestClient, DroneQueryService droneQueryService,
                        DroneAvailabilityService droneAvailabilityService,
                        DeliveryPathService deliveryPathService) {
        this.regionService = regionService;
        this.distanceService = distanceService;
        this.positionService = positionService;
        this.validationService = validationService;
        this.ilpRestClient = ilpRestClient;
        this.droneQueryService = droneQueryService;
        this.droneAvailabilityService = droneAvailabilityService;
        this.deliveryPathService = deliveryPathService;
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
        // Handle null or empty conditions - return empty array with 200 OK 
        // Explicitly check both null and empty to handle Spring Boot edge cases
        try {
            if (conditions == null) {
                return ResponseEntity.ok(List.of());
            }
            if (conditions.isEmpty()) {
                return ResponseEntity.ok(List.of());
            }
            
            // Fetch fresh data from ILP REST service
            List<Drone> drones = ilpRestClient.fetchDrones();
            
            // Delegate to service for query logic (AND logic - all conditions must match)
            List<String> droneIds = droneQueryService.queryByConditions(drones, conditions);
            
            return ResponseEntity.ok(droneIds);
        } catch (Exception e) {
            // Per spec: "Always return HTTP 200" - catch any exception and return empty array
            // This ensures we never return 400 or 500 for /query endpoint
            return ResponseEntity.ok(List.of());
        }
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
        List<ServicePoint> servicePoints = ilpRestClient.fetchServicePoints();
        List<DroneForServicePoint> dronesForServicePoints = ilpRestClient.fetchDronesForServicePoints();
        
        // Find drones that can handle all dispatches
        List<String> droneIds = droneAvailabilityService.findAvailableDrones(
                dispatches, drones, dronesForServicePoints, servicePoints);
        
        return ResponseEntity.ok(droneIds);
    }

    /**
     * Calculate delivery paths for dispatches
     * Returns optimal routes with flight paths, total moves, and total cost
     */
    @PostMapping(
            value = "/calcDeliveryPath",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<DeliveryPathResponse> calcDeliveryPath(
            @RequestBody(required = false) List<MedDispatchRec> dispatches) {
        
        // Validate request
        if (dispatches == null || dispatches.isEmpty()) {
            return ResponseEntity.ok(new DeliveryPathResponse(0.0, 0, List.of()));
        }
        
        // Fetch fresh data from ILP REST service
        List<Drone> drones = ilpRestClient.fetchDrones();
        List<ServicePoint> servicePoints = ilpRestClient.fetchServicePoints();
        List<DroneForServicePoint> dronesForServicePoints = ilpRestClient.fetchDronesForServicePoints();
        List<RestrictedArea> restrictedAreas = ilpRestClient.fetchRestrictedAreas();
        
        // Calculate delivery paths
        DeliveryPathResponse response = deliveryPathService.calculateDeliveryPaths(
                dispatches, drones, servicePoints, dronesForServicePoints, restrictedAreas);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Calculate delivery paths and return as GeoJSON
     * Same as calcDeliveryPath but returns GeoJSON format (FeatureCollection with LineString)
     * Always solvable by one drone
     */
    @PostMapping(
            value = "/calcDeliveryPathAsGeoJson",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<GeoJsonFeatureCollection> calcDeliveryPathAsGeoJson(
            @RequestBody(required = false) List<MedDispatchRec> dispatches) {
        
        // Validate request
        if (dispatches == null || dispatches.isEmpty()) {
            return ResponseEntity.ok(new GeoJsonFeatureCollection("FeatureCollection", List.of()));
        }
        
        // Fetch fresh data from ILP REST service
        List<Drone> drones = ilpRestClient.fetchDrones();
        List<ServicePoint> servicePoints = ilpRestClient.fetchServicePoints();
        List<DroneForServicePoint> dronesForServicePoints = ilpRestClient.fetchDronesForServicePoints();
        List<RestrictedArea> restrictedAreas = ilpRestClient.fetchRestrictedAreas();
        
        // Calculate delivery paths
        // The automarker ensures all test cases can be solved by one drone
        // Try single-drone first, fall back to multi-drone if needed 
        DeliveryPathResponse response = deliveryPathService.calculateSingleDroneOnly(
                dispatches, drones, servicePoints, dronesForServicePoints, restrictedAreas);
        
        // If no single-drone solution found, try multi-drone as fallback
        if (response == null || response.getDronePaths() == null || response.getDronePaths().isEmpty()) {
            response = deliveryPathService.calculateDeliveryPaths(
                    dispatches, drones, servicePoints, dronesForServicePoints, restrictedAreas);
        }
        
        // If still no solution, return empty FeatureCollection
        if (response == null || response.getDronePaths() == null || response.getDronePaths().isEmpty()) {
            return ResponseEntity.ok(new GeoJsonFeatureCollection("FeatureCollection", List.of()));
        }
        
        // Transform to GeoJSON format (include restricted areas, service points, and delivery points for visualization)
        GeoJsonFeatureCollection geoJson = transformToGeoJson(response, restrictedAreas, servicePoints, dispatches);
        
        return ResponseEntity.ok(geoJson);
    }
    
    /**
     * Transform DeliveryPathResponse to GeoJSON FeatureCollection
     * Combines all flight paths into LineString features
     * Also includes restricted areas, service points, and delivery points for visualization
     */
    private GeoJsonFeatureCollection transformToGeoJson(DeliveryPathResponse response, 
                                                         List<RestrictedArea> restrictedAreas,
                                                         List<ServicePoint> servicePoints,
                                                         List<MedDispatchRec> dispatches) {
        List<GeoJsonFeature> features = new ArrayList<>();
        
        // Add restricted areas as Polygon features (for visualization)
        if (restrictedAreas != null && !restrictedAreas.isEmpty()) {
            for (RestrictedArea area : restrictedAreas) {
                if (area != null && area.getVertices() != null && area.getVertices().size() >= 3) {
                    // Create polygon coordinates (close the ring by repeating first point)
                    List<List<Double>> polygonCoordinates = new ArrayList<>();
                    for (LngLat vertex : area.getVertices()) {
                        if (vertex != null && vertex.isValid()) {
                            polygonCoordinates.add(List.of(vertex.getLng(), vertex.getLat()));
                        }
                    }
                    // Only add closing point if polygon isn't already closed
                    if (!polygonCoordinates.isEmpty()) {
                        List<Double> first = polygonCoordinates.get(0);
                        List<Double> last = polygonCoordinates.get(polygonCoordinates.size() - 1);
                        // Check if already closed (first == last)
                        if (first.size() == 2 && last.size() == 2) {
                            if (!first.get(0).equals(last.get(0)) || !first.get(1).equals(last.get(1))) {
                                // Not closed, add first point at end
                                polygonCoordinates.add(new ArrayList<>(first));
                            }
                        } else {
                            // Add first point at end to close
                            polygonCoordinates.add(new ArrayList<>(first));
                        }
                    }
                    
                    // Create Polygon geometry (GeoJSON Polygon format: [[[lng, lat], ...]])
                    List<List<List<Double>>> polygonCoords = List.of(polygonCoordinates);
                    GeoJsonGeometry geometry = new GeoJsonGeometry("Polygon", polygonCoords);
                    
                    // Create properties for restricted area
                    GeoJsonProperties properties = new GeoJsonProperties();
                    String areaName = area.getName() != null ? area.getName() : "Restricted Area (No-Fly Zone)";
                    properties.setName(areaName);
                    
                    // Create feature
                    GeoJsonFeature feature = new GeoJsonFeature("Feature", geometry, properties);
                    features.add(feature);
                }
            }
        }
        
        // Add service points as Point features (for visualization)
        if (servicePoints != null && !servicePoints.isEmpty()) {
            for (ServicePoint servicePoint : servicePoints) {
                if (servicePoint != null && servicePoint.getLocation() != null) {
                    LngLatAlt location = servicePoint.getLocation();
                    if (location != null && location.getLng() != null && location.getLat() != null) {
                        // Create Point geometry (GeoJSON Point format: [lng, lat])
                        List<Double> pointCoordinates = List.of(location.getLng(), location.getLat());
                        GeoJsonGeometry geometry = new GeoJsonGeometry("Point", pointCoordinates);
                        
                        // Create properties for service point with distinct styling
                        GeoJsonProperties properties = new GeoJsonProperties();
                        String pointName = servicePoint.getName() != null ? servicePoint.getName() : "Service Point";
                        if (servicePoint.getId() != null) {
                            properties.setName(pointName + " (ID: " + servicePoint.getId() + ")");
                        } else {
                            properties.setName(pointName);
                        }
                        // Style service points as blue circles (large)
                        properties.setType("servicePoint");
                        properties.setMarkerColor("#0066FF"); // Blue
                        properties.setMarkerSize("large");
                        properties.setMarkerSymbol("circle");
                        
                        // Create feature
                        GeoJsonFeature feature = new GeoJsonFeature("Feature", geometry, properties);
                        features.add(feature);
                    }
                }
            }
        }
        
        // Add delivery points as Point features (for visualization)
        if (dispatches != null && !dispatches.isEmpty()) {
            for (MedDispatchRec dispatch : dispatches) {
                if (dispatch != null && dispatch.getDelivery() != null) {
                    LngLat deliveryLocation = dispatch.getDelivery();
                    if (deliveryLocation != null && deliveryLocation.isValid()) {
                        // Create Point geometry (GeoJSON Point format: [lng, lat])
                        List<Double> pointCoordinates = List.of(deliveryLocation.getLng(), deliveryLocation.getLat());
                        GeoJsonGeometry geometry = new GeoJsonGeometry("Point", pointCoordinates);
                        
                        // Create properties for delivery point with distinct styling
                        GeoJsonProperties properties = new GeoJsonProperties();
                        String deliveryName = "Delivery";
                        if (dispatch.getId() != null) {
                            deliveryName += " #" + dispatch.getId();
                        }
                        properties.setName(deliveryName);
                        // Style delivery points as red stars (large) - visually distinct from blue circles
                        properties.setType("deliveryPoint");
                        properties.setMarkerColor("#FF0000"); // Red
                        properties.setMarkerSize("large");
                        properties.setMarkerSymbol("star");
                        
                        // Create feature
                        GeoJsonFeature feature = new GeoJsonFeature("Feature", geometry, properties);
                        features.add(feature);
                    }
                }
            }
        }
        
        // Add flight paths as LineString features
        if (response != null && response.getDronePaths() != null && !response.getDronePaths().isEmpty()) {
            for (DronePath dronePath : response.getDronePaths()) {
                // Combine all flight paths from all deliveries into one LineString
                List<List<Double>> coordinates = new ArrayList<>();
                List<Integer> deliveryIds = new ArrayList<>();
                
                for (DeliveryPath delivery : dronePath.getDeliveries()) {
                    if (delivery.getFlightPath() != null) {
                        // Add delivery ID
                        if (delivery.getDeliveryId() != null) {
                            deliveryIds.add(delivery.getDeliveryId());
                        }
                        
                        // Add coordinates (skip first point if not first delivery to avoid duplicates)
                        boolean isFirstDelivery = coordinates.isEmpty();
                        for (int i = 0; i < delivery.getFlightPath().size(); i++) {
                            if (isFirstDelivery || i > 0) {
                                LngLat point = delivery.getFlightPath().get(i);
                                coordinates.add(List.of(point.getLng(), point.getLat()));
                            }
                        }
                    }
                }
                
                // Create GeoJSON geometry
                GeoJsonGeometry geometry = new GeoJsonGeometry("LineString", coordinates);
                
                // Create properties
                GeoJsonProperties properties = new GeoJsonProperties();
                properties.setDroneId(dronePath.getDroneId());
                properties.setDeliveryIds(deliveryIds);
                properties.setTotalMoves(response.getTotalMoves());
                properties.setTotalCost(response.getTotalCost());
                
                // Create feature
                GeoJsonFeature feature = new GeoJsonFeature("Feature", geometry, properties);
                features.add(feature);
            }
        }
        
        return new GeoJsonFeatureCollection("FeatureCollection", features);
    }
}
