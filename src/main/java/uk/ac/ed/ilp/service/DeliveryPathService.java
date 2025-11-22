package uk.ac.ed.ilp.service;

import org.springframework.stereotype.Service;
import uk.ac.ed.ilp.model.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for calculating delivery paths
 * Handles route optimization, move counting, and cost calculation
 */
@Service
public class DeliveryPathService {

    private final PathfindingService pathfindingService;
    private final DroneAvailabilityService droneAvailabilityService;
    private final DistanceService distanceService;

    public DeliveryPathService(PathfindingService pathfindingService,
                              DroneAvailabilityService droneAvailabilityService,
                              DistanceService distanceService) {
        this.pathfindingService = pathfindingService;
        this.droneAvailabilityService = droneAvailabilityService;
        this.distanceService = distanceService;
    }

    /**
     * Calculate delivery paths for dispatches
     * Finds optimal routes that minimize cost and moves
     */
    public DeliveryPathResponse calculateDeliveryPaths(
            List<MedDispatchRec> dispatches,
            List<Drone> allDrones,
            List<ServicePoint> servicePoints,
            List<DroneForServicePoint> dronesForServicePoints,
            List<RestrictedArea> restrictedAreas) {

        if (dispatches == null || dispatches.isEmpty()) {
            return new DeliveryPathResponse(0.0, 0, List.of());
        }

        // Find available drones that can handle all dispatches
        List<String> availableDroneIds = droneAvailabilityService.findAvailableDrones(
                dispatches, allDrones, dronesForServicePoints);

        if (availableDroneIds.isEmpty()) {
            // DEBUG: No available drones found
            return new DeliveryPathResponse(0.0, 0, List.of());
        }

        // Try to find a single drone that can handle all dispatches
        DeliveryPathResponse bestSolution = null;
        double bestCost = Double.MAX_VALUE;

        for (String droneId : availableDroneIds) {
            Drone drone = allDrones.stream()
                    .filter(d -> droneId.equals(d.getId()))
                    .findFirst()
                    .orElse(null);

            if (drone == null) continue;

            // Find service point for this drone
            ServicePoint servicePoint = findServicePointForDrone(droneId, servicePoints, dronesForServicePoints);
            if (servicePoint == null) {
                // Service point not found for drone - skip
                continue;
            }

            // Try to calculate path for all dispatches
            DeliveryPathResponse solution = calculatePathForDrone(
                    drone, servicePoint, dispatches, restrictedAreas);

            if (solution == null) {
                // Path calculation failed - could be:
                // 1. Path incomplete (pathfinding got stuck)
                // 2. Cannot reach delivery location
                // 3. Return path incomplete
                continue;
            }
            
            if (solution.getTotalMoves() > drone.getCapability().getMaxMoves()) {
                // Path exceeds max moves constraint
                continue;
            }
            
            if (solution.getTotalCost() < bestCost) {
                bestCost = solution.getTotalCost();
                bestSolution = solution;
            }
        }

        // If no single drone can handle all, try multiple drones
        if (bestSolution == null) {
            bestSolution = calculateMultiDroneSolution(
                    dispatches, allDrones, servicePoints, dronesForServicePoints, restrictedAreas);
        }

        return bestSolution != null ? bestSolution : new DeliveryPathResponse(0.0, 0, List.of());
    }

    /**
     * Calculate path for a single drone handling all dispatches
     */
    private DeliveryPathResponse calculatePathForDrone(
            Drone drone,
            ServicePoint servicePoint,
            List<MedDispatchRec> dispatches,
            List<RestrictedArea> restrictedAreas) {

        LngLatAlt location = servicePoint.getLocation();
        if (location == null) {
            return null;
        }
        LngLat startPoint = new LngLat(location.getLng(), location.getLat());

        List<DeliveryPath> deliveryPaths = new ArrayList<>();
        int totalMoves = 0;
        LngLat currentPosition = new LngLat(startPoint.getLng(), startPoint.getLat());

        // Visit dispatches in order
        for (MedDispatchRec dispatch : dispatches) {
            if (dispatch.getDelivery() == null) continue;

            LngLat deliveryLocation = dispatch.getDelivery();

            // Calculate path from current position to delivery
            List<LngLat> pathToDelivery = pathfindingService.calculatePath(
                    currentPosition, deliveryLocation, restrictedAreas);

            if (pathToDelivery.isEmpty()) {
                return null; // Cannot reach delivery
            }

            // Validate path completeness - must actually reach destination
            LngLat lastPoint = pathToDelivery.get(pathToDelivery.size() - 1);
            if (!distanceService.areClose(lastPoint, deliveryLocation)) {
                // Path is incomplete - pathfinding got stuck
                return null; // Cannot complete path to delivery
            }
            // Path is complete - destination is already in path (added by pathfinding when reached)
            
            // Add hover at delivery point: two identical coordinates (per Piazza clarification)
            // The hover represents the delivery and counts as 1 move
            LngLat hoverPoint = new LngLat(lastPoint.getLng(), lastPoint.getLat());
            pathToDelivery.add(hoverPoint); // Add duplicate coordinate for hover

            // Count moves for this segment (including hover)
            int moves = pathfindingService.countMoves(pathToDelivery);
            totalMoves += moves;

            // Create delivery path
            DeliveryPath deliveryPath = new DeliveryPath();
            deliveryPath.setDeliveryId(dispatch.getId());
            deliveryPath.setFlightPath(pathToDelivery);
            deliveryPaths.add(deliveryPath);

            // Update current position (use the hover point as starting point for next delivery)
            currentPosition = new LngLat(hoverPoint.getLng(), hoverPoint.getLat());
        }

        // Calculate path back to service point and add to last delivery
        List<LngLat> pathBack = pathfindingService.calculatePath(
                currentPosition, startPoint, restrictedAreas);
        if (!pathBack.isEmpty() && !deliveryPaths.isEmpty()) {
            // Validate return path completeness - must actually reach service point
            LngLat lastReturnPoint = pathBack.get(pathBack.size() - 1);
            if (!distanceService.areClose(lastReturnPoint, startPoint)) {
                // Return path is incomplete - cannot complete return
                return null;
            }
            
            // Add return path to last delivery's flight path
            // pathBack includes the starting point (currentPosition/hover), so we skip the first point
            // to avoid duplicating the hover point
            DeliveryPath lastDelivery = deliveryPaths.get(deliveryPaths.size() - 1);
            List<LngLat> lastPath = new ArrayList<>(lastDelivery.getFlightPath());
            if (pathBack.size() > 1) {
                // Skip first point (already in lastPath as hover) and add the rest
                lastPath.addAll(pathBack.subList(1, pathBack.size()));
            } else {
                // If pathBack only has one point, it's the same as current position (hover)
                // Just add it to ensure we have the return point
                lastPath.addAll(pathBack);
            }
            lastDelivery.setFlightPath(lastPath);
            
            // Recalculate total moves from the complete combined path
            // For multiple deliveries, we need to combine ALL delivery paths
            List<LngLat> combinedPath = new ArrayList<>();
            for (DeliveryPath dp : deliveryPaths) {
                List<LngLat> flightPath = dp.getFlightPath();
                if (combinedPath.isEmpty()) {
                    combinedPath.addAll(flightPath);
                } else {
                    // Skip first point of subsequent paths (already in combinedPath)
                    if (flightPath.size() > 1) {
                        combinedPath.addAll(flightPath.subList(1, flightPath.size()));
                    }
                }
            }
            totalMoves = pathfindingService.countMoves(combinedPath);
        }

        // Check if exceeds max moves
        if (totalMoves > drone.getCapability().getMaxMoves()) {
            return null;
        }

        // Calculate cost
        double cost = calculateCost(drone, totalMoves);

        // Create drone path
        DronePath dronePath = new DronePath();
        dronePath.setDroneId(drone.getId());
        dronePath.setDeliveries(deliveryPaths);

        DeliveryPathResponse response = new DeliveryPathResponse();
        response.setTotalCost(cost);
        response.setTotalMoves(totalMoves);
        response.setDronePaths(List.of(dronePath));

        return response;
    }

    /**
     * Calculate multi-drone solution (if single drone can't handle all)
     * Groups dispatches by date and assigns them to different drones
     */
    private DeliveryPathResponse calculateMultiDroneSolution(
            List<MedDispatchRec> dispatches,
            List<Drone> allDrones,
            List<ServicePoint> servicePoints,
            List<DroneForServicePoint> dronesForServicePoints,
            List<RestrictedArea> restrictedAreas) {

        if (dispatches == null || dispatches.isEmpty()) {
            return new DeliveryPathResponse(0.0, 0, List.of());
        }

        // Group dispatches by date (each day is a new game per Piazza @150)
        Map<String, List<MedDispatchRec>> dispatchesByDate = dispatches.stream()
                .collect(Collectors.groupingBy(
                        dispatch -> dispatch.getDate() != null ? dispatch.getDate() : "no-date",
                        Collectors.toList()
                ));

        List<DronePath> allDronePaths = new ArrayList<>();
        double totalCost = 0.0;
        int totalMoves = 0;

        // Process each date group separately
        for (Map.Entry<String, List<MedDispatchRec>> dateEntry : dispatchesByDate.entrySet()) {
            List<MedDispatchRec> dateDispatches = dateEntry.getValue();
            
            // Assign dispatches to drones (greedy assignment)
            Map<String, List<MedDispatchRec>> droneAssignments = assignDispatchesToDrones(
                    dateDispatches, allDrones, dronesForServicePoints);

            // Calculate paths for each drone's assigned dispatches
            for (Map.Entry<String, List<MedDispatchRec>> assignment : droneAssignments.entrySet()) {
                String droneId = assignment.getKey();
                List<MedDispatchRec> assignedDispatches = assignment.getValue();

                Drone drone = allDrones.stream()
                        .filter(d -> droneId.equals(d.getId()))
                        .findFirst()
                        .orElse(null);

                if (drone == null) continue;

                ServicePoint servicePoint = findServicePointForDrone(droneId, servicePoints, dronesForServicePoints);
                if (servicePoint == null) continue;

                // Calculate path for this drone's dispatches
                DeliveryPathResponse droneSolution = calculatePathForDrone(
                        drone, servicePoint, assignedDispatches, restrictedAreas);

                if (droneSolution != null && !droneSolution.getDronePaths().isEmpty()) {
                    // Add this drone's path
                    allDronePaths.addAll(droneSolution.getDronePaths());
                    totalCost += droneSolution.getTotalCost();
                    totalMoves += droneSolution.getTotalMoves();
                }
            }
        }

        if (allDronePaths.isEmpty()) {
            return new DeliveryPathResponse(0.0, 0, List.of());
        }

        return new DeliveryPathResponse(totalCost, totalMoves, allDronePaths);
    }

    /**
     * Assign dispatches to drones using greedy approach
     * Tries to assign multiple dispatches to the same drone when possible
     * Returns map of droneId -> list of dispatches assigned to that drone
     */
    private Map<String, List<MedDispatchRec>> assignDispatchesToDrones(
            List<MedDispatchRec> dispatches,
            List<Drone> allDrones,
            List<DroneForServicePoint> dronesForServicePoints) {

        Map<String, List<MedDispatchRec>> assignments = new HashMap<>();
        Set<Integer> assignedDispatchIds = new HashSet<>();

        // Try to assign multiple dispatches to the same drone (optimize drone usage)
        for (MedDispatchRec dispatch : dispatches) {
            if (assignedDispatchIds.contains(dispatch.getId())) {
                continue; // Already assigned
            }

            // Find drones that can handle this single dispatch
            List<String> availableDrones = droneAvailabilityService.findAvailableDrones(
                    List.of(dispatch), allDrones, dronesForServicePoints);

            if (availableDrones.isEmpty()) {
                continue; // No drone can handle this dispatch
            }

            // Try to assign to an existing drone that can also handle this dispatch
            // (to minimize number of drones used)
            boolean assigned = false;
            for (Map.Entry<String, List<MedDispatchRec>> existing : assignments.entrySet()) {
                String droneId = existing.getKey();
                List<MedDispatchRec> existingDispatches = existing.getValue();
                
                // Check if this drone can handle all existing dispatches + new dispatch
                List<MedDispatchRec> combined = new ArrayList<>(existingDispatches);
                combined.add(dispatch);
                
                List<String> canHandleAll = droneAvailabilityService.findAvailableDrones(
                        combined, allDrones, dronesForServicePoints);
                
                if (canHandleAll.contains(droneId)) {
                    // This drone can handle all dispatches including the new one
                    existingDispatches.add(dispatch);
                    assignedDispatchIds.add(dispatch.getId());
                    assigned = true;
                    break;
                }
            }

            // If not assigned to existing drone, assign to first available new drone
            if (!assigned) {
                String droneId = availableDrones.get(0);
                assignments.computeIfAbsent(droneId, k -> new ArrayList<>()).add(dispatch);
                assignedDispatchIds.add(dispatch.getId());
            }
        }

        return assignments;
    }

    /**
     * Find service point for a drone
     */
    private ServicePoint findServicePointForDrone(
            String droneId,
            List<ServicePoint> servicePoints,
            List<DroneForServicePoint> dronesForServicePoints) {

        if (dronesForServicePoints == null || servicePoints == null) {
            return null;
        }

        for (DroneForServicePoint sp : dronesForServicePoints) {
            if (sp != null && sp.getDrones() != null) {
                for (DroneAvailabilityInfo info : sp.getDrones()) {
                    if (info != null && droneId.equals(info.getId())) {
                        // Find the service point
                        return servicePoints.stream()
                                .filter(spoint -> spoint.getId().equals(sp.getServicePointId()))
                                .findFirst()
                                .orElse(null);
                    }
                }
            }
        }

        return null;
    }

    /**
     * Calculate cost for a drone's path
     * cost = costInitial + (costPerMove * moves) + costFinal
     */
    private double calculateCost(Drone drone, int moves) {
        if (drone == null || drone.getCapability() == null) {
            return 0.0;
        }

        DroneCapability cap = drone.getCapability();
        double costInitial = cap.getCostInitial() != null ? cap.getCostInitial() : 0.0;
        double costPerMove = cap.getCostPerMove() != null ? cap.getCostPerMove() : 0.0;
        double costFinal = cap.getCostFinal() != null ? cap.getCostFinal() : 0.0;

        return costInitial + (costPerMove * moves) + costFinal;
    }
}

