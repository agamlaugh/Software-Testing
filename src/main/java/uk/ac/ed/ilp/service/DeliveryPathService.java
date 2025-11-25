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

        // Check if dispatches have different dates
        Set<String> uniqueDates = dispatches.stream()
                .filter(d -> d.getDate() != null)
                .map(MedDispatchRec::getDate)
                .collect(Collectors.toSet());
        
        // If multiple dates, group by date and process separately (per Piazza @150)
        if (uniqueDates.size() > 1) {
            return calculateMultiDroneSolution(
                    dispatches, allDrones, servicePoints, dronesForServicePoints, restrictedAreas);
        }

        // Single date: Try to find a single drone that can handle all dispatches
        List<String> availableDroneIds = droneAvailabilityService.findAvailableDrones(
                dispatches, allDrones, dronesForServicePoints, servicePoints);

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
                continue;
            }

            // Calculate path for all dispatches using Greedy TSP ordering
            DeliveryPathResponse solution = calculatePathForDrone(
                    drone, servicePoint, new ArrayList<>(dispatches), restrictedAreas);

            if (solution == null) {
                continue;
            }
            
            // Check max moves
            if (drone.getCapability() != null && solution.getTotalMoves() > drone.getCapability().getMaxMoves()) {
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
     * Calculate delivery paths using only single-drone solutions
     * Returns null if no single-drone solution exists
     */
    public DeliveryPathResponse calculateSingleDroneOnly(
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
            if (servicePoint == null) continue;

            // Calculate path for all dispatches using Greedy TSP ordering
            DeliveryPathResponse solution = calculatePathForDrone(
                    drone, servicePoint, new ArrayList<>(dispatches), restrictedAreas);

            if (solution == null) continue;
            
            // Check max moves
            if (drone.getCapability() != null && solution.getTotalMoves() > drone.getCapability().getMaxMoves()) {
                continue;
            }

            // Found a valid single-drone solution
            if (solution.getTotalCost() < bestCost) {
                bestCost = solution.getTotalCost();
                bestSolution = solution;
            }
        }

        // Return single-drone solution or null (no multi-drone fallback)
        return bestSolution;
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
                return null; // Unreachable
            }

            // Validate path completeness
            LngLat lastPoint = pathToDelivery.get(pathToDelivery.size() - 1);
            if (!distanceService.areClose(lastPoint, deliveryLocation)) {
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
        if (drone.getCapability() != null && totalMoves > drone.getCapability().getMaxMoves()) {
            return null;
        }

        // Calculate cost
        double cost = calculateCost(drone, totalMoves);
        
        // Check maxCost validation (per delivery)
        // EACH delivery's cost per delivery must be <= its maxCost
        int numDeliveries = dispatches.size();
        if (numDeliveries > 0) {
            double costPerDelivery = cost / numDeliveries;
            
            for (MedDispatchRec dispatch : dispatches) {
                if (dispatch.getRequirements() != null && dispatch.getRequirements().getMaxCost() != null) {
                    if (costPerDelivery > dispatch.getRequirements().getMaxCost()) {
                        return null;
                    }
                }
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
            
            // For multi-drone scenarios, we need to consider ALL drones available at the date/time
            // with the right capabilities, not just those that can handle a delivery individually.
            // This is because maxCost per delivery changes when deliveries are combined.
            
            // Get common date/time for all dispatches (they should all be the same date)
            String commonDate = dateDispatches.stream()
                    .filter(d -> d.getDate() != null)
                    .map(MedDispatchRec::getDate)
                    .findFirst()
                    .orElse(null);
            String commonTime = dateDispatches.stream()
                    .filter(d -> d.getTime() != null)
                    .map(MedDispatchRec::getTime)
                    .findFirst()
                    .orElse(null);
            
            // Get all drones that:
            // 1. Are available at service points
            // 2. Are available at the date/time (if provided)
            // 3. Have capabilities matching at least one dispatch (heating/cooling, ignoring maxCost for now)
            Set<String> requiredCapabilities = new HashSet<>();
            for (MedDispatchRec d : dateDispatches) {
                if (d.getRequirements() != null) {
                    if (Boolean.TRUE.equals(d.getRequirements().getHeating())) {
                        requiredCapabilities.add("heating");
                    }
                    if (Boolean.TRUE.equals(d.getRequirements().getCooling())) {
                        requiredCapabilities.add("cooling");
                    }
                }
            }
            
            // Filter drones: must be available at date/time and have required capabilities
            List<Drone> availableDrones = allDrones.stream()
                    .filter(drone -> {
                        // Check if drone is available at service points
                        if (findServicePointForDrone(drone.getId(), servicePoints, dronesForServicePoints) == null) {
                            return false;
                        }
                        
                        // Check date/time availability if provided
                        if (commonDate != null && commonTime != null) {
                            if (!droneAvailabilityService.isDroneAvailableAtDateTime(
                                    drone, commonDate, commonTime, dronesForServicePoints)) {
                                return false;
                            }
                        }
                        
                        // Check if drone has required capabilities
                        // IMPORTANT: If we have both heating and cooling requirements, we need drones that can handle
                        // at least ONE of them (they'll be assigned separately). If we only have one type of requirement,
                        // the drone must have that capability.
                        if (drone.getCapability() != null) {
                            boolean hasHeatingReq = requiredCapabilities.contains("heating");
                            boolean hasCoolingReq = requiredCapabilities.contains("cooling");
                            boolean droneHasHeating = Boolean.TRUE.equals(drone.getCapability().getHeating());
                            boolean droneHasCooling = Boolean.TRUE.equals(drone.getCapability().getCooling());
                            
                            // If we have both heating and cooling requirements, drone must have at least one
                            if (hasHeatingReq && hasCoolingReq) {
                                if (!droneHasHeating && !droneHasCooling) {
                                    return false; // Drone has neither, can't help
                                }
                                // Drone has at least one - it can handle deliveries that match its capability
                            } else if (hasHeatingReq) {
                                // Only heating required - drone must have heating
                                if (!droneHasHeating) {
                                    return false;
                                }
                            } else if (hasCoolingReq) {
                                // Only cooling required - drone must have cooling
                                if (!droneHasCooling) {
                                    return false;
                                }
                            }
                        }
                        
                        return true;
                    })
                    .distinct()
                    .sorted((d1, d2) -> {
                        // Sort by capacity descending
                        Double c1 = (d1.getCapability() != null) ? d1.getCapability().getCapacity() : null;
                        Double c2 = (d2.getCapability() != null) ? d2.getCapability().getCapacity() : null;
                        return Double.compare(c2 == null ? 0 : c2, c1 == null ? 0 : c1);
                    })
                    .collect(Collectors.toList());
            
            List<MedDispatchRec> unassigned = new ArrayList<>(dateDispatches);
            
            for (Drone drone : availableDrones) {
                if (unassigned.isEmpty()) break;

                ServicePoint servicePoint = findServicePointForDrone(drone.getId(), servicePoints, dronesForServicePoints);
                if (servicePoint == null) continue;

                List<MedDispatchRec> assignedToThisDrone = new ArrayList<>();
                
                // --- IMPROVED GREEDY PACKING ---
                // Problem: Individual deliveries might fail maxCost check, but combinations work
                // Solution: Try to find the largest valid subset of remaining deliveries
                
                List<MedDispatchRec> candidates = new ArrayList<>(unassigned);
                List<MedDispatchRec> currentRoute = new ArrayList<>();
                
                // Strategy: Try to find the largest subset that works together
                // Start by trying all remaining deliveries together, then reduce if needed
                while (!candidates.isEmpty()) {
                    // Try to add deliveries one at a time first (greedy)
                    boolean addedSomething = true;
                    while (!candidates.isEmpty() && addedSomething) {
                        addedSomething = false;
                        
                        Iterator<MedDispatchRec> it = candidates.iterator();
                        while (it.hasNext()) {
                            MedDispatchRec candidate = it.next();
                            List<MedDispatchRec> trialRoute = new ArrayList<>(currentRoute);
                            trialRoute.add(candidate);
                            
                            // Check Capacity and other requirements
                            String failureReason = droneAvailabilityService.canDroneHandleDispatchesWithReason(
                                    drone, trialRoute, dronesForServicePoints, servicePoints);
                            
                            if (failureReason != null) {
                                continue; // Skip this candidate
                            }
                                        
                            // Check Path feasibility (moves & maxCost)
                            DeliveryPathResponse trialSolution = calculatePathForDrone(
                                    drone, servicePoint, trialRoute, restrictedAreas);
                                    
                            if (trialSolution != null) {
                                // It fits!
                                currentRoute.add(candidate);
                                it.remove();
                                addedSomething = true;
                            }
                        }
                    }
                    
                    // If we couldn't add any single delivery, try combinations
                    // This handles the case where individual deliveries fail maxCost but combinations work
                    if (currentRoute.isEmpty() && !candidates.isEmpty()) {
                        // Try all remaining candidates together
                        String failureReason = droneAvailabilityService.canDroneHandleDispatchesWithReason(
                                drone, candidates, dronesForServicePoints, servicePoints);
                        
                        if (failureReason == null) {
                            DeliveryPathResponse trialSolution = calculatePathForDrone(
                                    drone, servicePoint, candidates, restrictedAreas);
                            
                            if (trialSolution != null) {
                                // All remaining candidates work together!
                                currentRoute.addAll(candidates);
                                candidates.clear();
                                break;
                            }
                        }
                        
                        // If all together doesn't work, try pairs
                        boolean foundPair = false;
                        for (int i = 0; i < candidates.size() && !foundPair; i++) {
                            for (int j = i + 1; j < candidates.size() && !foundPair; j++) {
                                MedDispatchRec d1 = candidates.get(i);
                                MedDispatchRec d2 = candidates.get(j);
                                List<MedDispatchRec> pair = List.of(d1, d2);
                                
                                failureReason = droneAvailabilityService.canDroneHandleDispatchesWithReason(
                                        drone, pair, dronesForServicePoints, servicePoints);
                                
                                if (failureReason == null) {
                                    DeliveryPathResponse pairSolution = calculatePathForDrone(
                                            drone, servicePoint, pair, restrictedAreas);
                                    
                                    if (pairSolution != null) {
                                        currentRoute.addAll(pair);
                                        candidates.removeAll(pair);
                                        foundPair = true;
                                    }
                                }
                            }
                        }
                        
                        // If no combination works, break
                        if (!foundPair) {
                            break;
                        }
                    } else {
                        // We added something, break to process this route
                        break;
                    }
                }
                
                if (!currentRoute.isEmpty()) {
                    // We formed a valid route
                    assignedToThisDrone.addAll(currentRoute);
                    unassigned.removeAll(currentRoute);
                    
                    // Calculate final solution for this drone
                    DeliveryPathResponse droneSolution = calculatePathForDrone(
                            drone, servicePoint, currentRoute, restrictedAreas);
                    
                    if (droneSolution == null) {
                        continue;
                    }
                            
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
