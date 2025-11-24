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
                dispatches, allDrones, dronesForServicePoints, servicePoints);

        System.out.println("[DEBUG] Found " + availableDroneIds.size() + " potential drones for single-drone solution: " + availableDroneIds);

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
                System.out.println("[DEBUG] Drone " + droneId + " skipped: No ServicePoint found");
                continue;
            }

            System.out.println("[DEBUG] Trying drone " + droneId + " from " + servicePoint.getName() + " (MaxMoves: " + drone.getCapability().getMaxMoves() + ")");

            // Calculate path for all dispatches using Greedy TSP ordering
            DeliveryPathResponse solution = calculatePathForDrone(
                    drone, servicePoint, new ArrayList<>(dispatches), restrictedAreas);

            if (solution == null) {
                System.out.println("[DEBUG] Drone " + droneId + " failed: path calculation returned null (unreachable, return path failed, or cost > maxCost)");
                continue;
            }
            
            // Check max moves
            if (solution.getTotalMoves() > drone.getCapability().getMaxMoves()) {
                System.out.println("[DEBUG] Drone " + droneId + " failed: Total Moves " + solution.getTotalMoves() + " > Max " + drone.getCapability().getMaxMoves());
                continue;
            }
            
            System.out.println("[DEBUG] Drone " + droneId + " VALID solution found! Cost: " + solution.getTotalCost() + ", Moves: " + solution.getTotalMoves());

            if (solution.getTotalCost() < bestCost) {
                bestCost = solution.getTotalCost();
                bestSolution = solution;
            }
        }

        // If no single drone can handle all, try multiple drones
        if (bestSolution == null) {
            System.out.println("[DEBUG] No single drone solution found. Attempting multi-drone solution...");
            bestSolution = calculateMultiDroneSolution(
                    dispatches, allDrones, servicePoints, dronesForServicePoints, restrictedAreas);
        }

        return bestSolution != null ? bestSolution : new DeliveryPathResponse(0.0, 0, List.of());
    }

    /**
     * Calculate path for a single drone handling all dispatches
     * USES GREEDY TSP (Nearest Neighbor) for delivery order
     */
    private DeliveryPathResponse calculatePathForDrone(
            Drone drone,
            ServicePoint servicePoint,
            List<MedDispatchRec> dispatches,
            List<RestrictedArea> restrictedAreas) {

        LngLatAlt location = servicePoint.getLocation();
        if (location == null) return null;
        LngLat startPoint = new LngLat(location.getLng(), location.getLat());

        List<DeliveryPath> deliveryPaths = new ArrayList<>();
        int totalMoves = 0;
        LngLat currentPosition = new LngLat(startPoint.getLng(), startPoint.getLat());

        // --- GREEDY TSP (Nearest Neighbor) ---
        List<MedDispatchRec> unvisited = new ArrayList<>(dispatches);
        
        while (!unvisited.isEmpty()) {
            // Find nearest unvisited dispatch
            MedDispatchRec nearest = null;
            double minDistance = Double.MAX_VALUE;
            
            for (MedDispatchRec dispatch : unvisited) {
                if (dispatch.getDelivery() == null) continue;
                double dist = distanceService.calculateDistance(currentPosition, dispatch.getDelivery());
                if (dist < minDistance) {
                    minDistance = dist;
                    nearest = dispatch;
                }
            }
            
            if (nearest == null) break; // Should not happen if inputs are valid
            unvisited.remove(nearest);
            
            LngLat deliveryLocation = nearest.getDelivery();
            
            // Calculate path using A*
            List<LngLat> pathToDelivery = pathfindingService.calculatePath(
                    currentPosition, deliveryLocation, restrictedAreas);

            if (pathToDelivery.isEmpty()) {
                System.out.println("[DEBUG] A* Failed: Cannot find path from " + currentPosition.getLng() + "," + currentPosition.getLat() + 
                                   " to " + deliveryLocation.getLng() + "," + deliveryLocation.getLat());
                return null; // Unreachable
            }

            // Validate path completeness
            LngLat lastPoint = pathToDelivery.get(pathToDelivery.size() - 1);
            if (!distanceService.areClose(lastPoint, deliveryLocation)) {
                System.out.println("[DEBUG] A* Failed: Path incomplete. Last point " + lastPoint.getLng() + "," + lastPoint.getLat() + 
                                   " is not close to " + deliveryLocation.getLng() + "," + deliveryLocation.getLat());
                return null;
            }

            // Add hover
            LngLat hoverPoint = new LngLat(lastPoint.getLng(), lastPoint.getLat());
            pathToDelivery.add(hoverPoint);

            // Count moves
            int moves = pathfindingService.countMoves(pathToDelivery);
            totalMoves += moves;

            // Record path
            DeliveryPath deliveryPath = new DeliveryPath();
            deliveryPath.setDeliveryId(nearest.getId());
            deliveryPath.setFlightPath(pathToDelivery);
            deliveryPaths.add(deliveryPath);

            // Update current pos
            currentPosition = hoverPoint;
        }

        // Return to service point
        List<LngLat> pathBack = pathfindingService.calculatePath(
                currentPosition, startPoint, restrictedAreas);
                
        if (!pathBack.isEmpty() && !deliveryPaths.isEmpty()) {
            LngLat lastReturnPoint = pathBack.get(pathBack.size() - 1);
            if (!distanceService.areClose(lastReturnPoint, startPoint)) {
                System.out.println("[DEBUG] A* Failed: Cannot find return path to " + startPoint);
                return null;
            }
            
            // Add return path to last delivery
            DeliveryPath lastDelivery = deliveryPaths.get(deliveryPaths.size() - 1);
            List<LngLat> lastPath = new ArrayList<>(lastDelivery.getFlightPath());
            if (pathBack.size() > 1) {
                lastPath.addAll(pathBack.subList(1, pathBack.size()));
            } else {
                lastPath.addAll(pathBack);
            }
            lastDelivery.setFlightPath(lastPath);
            
            // Recalculate total moves combining all paths
            List<LngLat> combinedPath = new ArrayList<>();
            for (DeliveryPath dp : deliveryPaths) {
                List<LngLat> flightPath = dp.getFlightPath();
                if (combinedPath.isEmpty()) {
                    combinedPath.addAll(flightPath);
                } else {
                    if (flightPath.size() > 1) {
                        combinedPath.addAll(flightPath.subList(1, flightPath.size()));
                    }
                }
            }
            totalMoves = pathfindingService.countMoves(combinedPath);
        }

        // Validate max moves
        if (totalMoves > drone.getCapability().getMaxMoves()) {
            return null;
        }

        // Calculate cost
        double cost = calculateCost(drone, totalMoves);
        
        // Check maxCost validation (pooled budget for multi-delivery)
        // For a single delivery, this checks cost <= maxCost.
        // For multiple deliveries, we sum the maxCosts to create a pooled budget.
        double totalMaxBudget = 0.0;
        boolean anyMaxCostDefined = false;
        
        for (MedDispatchRec dispatch : dispatches) {
            if (dispatch.getRequirements() != null && dispatch.getRequirements().getMaxCost() != null) {
                totalMaxBudget += dispatch.getRequirements().getMaxCost();
                anyMaxCostDefined = true;
            } else {
                // If a dispatch has no maxCost limit, we can assume it can accept any cost?
                // Or strictly follow provided limits? 
                // For safety, we'll assume missing maxCost doesn't contribute to the limit (conservative)
                // unless we decide otherwise.
            }
        }

        if (anyMaxCostDefined) {
            if (cost > totalMaxBudget) {
                System.out.println("[DEBUG] Cost Check Failed: Total Cost " + cost + " > Pooled Budget " + totalMaxBudget);
                return null;
            }
        }

        // Build response
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
     * Calculate multi-drone solution using GREEDY PACKING
     * Dispatches are sorted by date first, then processed
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

        // Group by date
        Map<String, List<MedDispatchRec>> dispatchesByDate = dispatches.stream()
                .collect(Collectors.groupingBy(
                        d -> d.getDate() != null ? d.getDate() : "no-date",
                        Collectors.toList()
                ));

        List<DronePath> allDronePaths = new ArrayList<>();
        double totalCost = 0.0;
        int totalMoves = 0;

        for (List<MedDispatchRec> dateDispatches : dispatchesByDate.values()) {
            
            // Get all available drones for this date (that can handle at least one dispatch)
            // Ideally, we sort drones by capacity descending to use big drones first
            List<String> potentialDroneIds = new ArrayList<>();
            for (MedDispatchRec d : dateDispatches) {
                potentialDroneIds.addAll(droneAvailabilityService.findAvailableDrones(
                        List.of(d), allDrones, dronesForServicePoints, servicePoints));
            }
            
            List<Drone> availableDrones = allDrones.stream()
                    .filter(d -> potentialDroneIds.contains(d.getId()))
                    .distinct()
                    .sorted((d1, d2) -> {
                        // Sort by capacity descending
                        Double c1 = d1.getCapability().getCapacity();
                        Double c2 = d2.getCapability().getCapacity();
                        return Double.compare(c2 == null ? 0 : c2, c1 == null ? 0 : c1);
                    })
                    .collect(Collectors.toList());

            List<MedDispatchRec> unassigned = new ArrayList<>(dateDispatches);
            
            for (Drone drone : availableDrones) {
                if (unassigned.isEmpty()) break;

                ServicePoint servicePoint = findServicePointForDrone(drone.getId(), servicePoints, dronesForServicePoints);
                if (servicePoint == null) continue;

                List<MedDispatchRec> assignedToThisDrone = new ArrayList<>();
                
                // --- GREEDY PACKING ---
                // 1. Find nearest dispatch to start
                // 2. Try to add it. If valid path & valid cost/capacity -> keep it.
                // 3. Repeat for next nearest from current location.
                
                // We build the route incrementally
                List<MedDispatchRec> candidates = new ArrayList<>(unassigned);
                List<MedDispatchRec> currentRoute = new ArrayList<>();
                
                while (!candidates.isEmpty()) {
                    // Calculate path for current route to check feasibility
                    // This is expensive but ensures validity
                    
                    // Pick nearest neighbor to add to route
                    MedDispatchRec bestCandidate = null;
                    // (Simplified: just iterate and try to add)
                    // For true greedy, we should calculate distance from last point
                    
                    // Let's iterate through candidates and try to add the first one that fits
                    // Optimization: Sort candidates by distance from Service Point (initially) or last delivery
                    
                    // Just try to pack as many as possible
                    Iterator<MedDispatchRec> it = candidates.iterator();
                    while (it.hasNext()) {
                        MedDispatchRec candidate = it.next();
                        List<MedDispatchRec> trialRoute = new ArrayList<>(currentRoute);
                        trialRoute.add(candidate);
                        
                        // Check Capacity
                        if (droneAvailabilityService.canDroneHandleDispatches(
                                drone, trialRoute, dronesForServicePoints, servicePoints)) {
                                    
                            // Check Path feasibility (moves & maxCost)
                            DeliveryPathResponse trialSolution = calculatePathForDrone(
                                    drone, servicePoint, trialRoute, restrictedAreas);
                                    
                            if (trialSolution != null) {
                                // It fits!
                                currentRoute.add(candidate);
                                it.remove(); // Remove from candidates for this drone
                                // Also remove from main unassigned list later
                            }
                        }
                    }
                    // If we couldn't add any more candidates, break loop for this drone
                    break; 
                }
                
                if (!currentRoute.isEmpty()) {
                    // We formed a valid route
                    assignedToThisDrone.addAll(currentRoute);
                    unassigned.removeAll(currentRoute);
                    
                    // Calculate final solution for this drone
                    DeliveryPathResponse droneSolution = calculatePathForDrone(
                            drone, servicePoint, currentRoute, restrictedAreas);
                            
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

    private ServicePoint findServicePointForDrone(
            String droneId,
            List<ServicePoint> servicePoints,
            List<DroneForServicePoint> dronesForServicePoints) {
        if (dronesForServicePoints == null || servicePoints == null) return null;
        for (DroneForServicePoint sp : dronesForServicePoints) {
            if (sp != null && sp.getDrones() != null) {
                for (DroneAvailabilityInfo info : sp.getDrones()) {
                    if (info != null && droneId.equals(info.getId())) {
                        return servicePoints.stream()
                                .filter(p -> p.getId().equals(sp.getServicePointId()))
                                .findFirst().orElse(null);
                    }
                }
            }
        }
        return null;
    }

    private double calculateCost(Drone drone, int moves) {
        if (drone == null || drone.getCapability() == null) return 0.0;
        DroneCapability c = drone.getCapability();
        return (c.getCostInitial() == null ? 0 : c.getCostInitial()) +
               (c.getCostPerMove() == null ? 0 : c.getCostPerMove()) * moves +
               (c.getCostFinal() == null ? 0 : c.getCostFinal());
    }
}
