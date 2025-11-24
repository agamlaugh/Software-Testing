package uk.ac.ed.ilp.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.ac.ed.ilp.model.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for finding drones available for medical dispatches
 * Handles capability matching, date/time availability, and service point matching
 */
@Service
public class DroneAvailabilityService {

    private final DistanceService distanceService;

    @Autowired
    public DroneAvailabilityService(DistanceService distanceService) {
        this.distanceService = distanceService;
    }

    /**
     * Find drones that can handle all dispatches
     * All dispatches must be matchable by ONE drone (AND logic)
     * 
     * @param dispatches List of medical dispatch records
     * @param allDrones All available drones
     * @param dronesForServicePoints Drone availability at service points
     * @param servicePoints Service points (needed for maxCost distance estimation)
     * @return List of drone IDs that can handle all dispatches
     */
    public List<String> findAvailableDrones(
            List<MedDispatchRec> dispatches,
            List<Drone> allDrones,
            List<DroneForServicePoint> dronesForServicePoints,
            List<ServicePoint> servicePoints) {
        
        if (dispatches == null || dispatches.isEmpty()) {
            return List.of();
        }
        
        // Get all drone IDs available at service points
        Set<String> availableDroneIds = getAvailableDroneIds(dronesForServicePoints);
        
        // Filter to only drones available at service points
        List<Drone> servicePointDrones = allDrones.stream()
                .filter(drone -> drone != null && drone.getId() != null)
                .filter(drone -> availableDroneIds.contains(drone.getId()))
                .collect(Collectors.toList());
        
        // Find drones that can handle ALL dispatches
        List<String> matchingDrones = servicePointDrones.stream()
                .filter(drone -> canHandleAllDispatches(drone, dispatches, dronesForServicePoints, servicePoints))
                .map(Drone::getId)
                .collect(Collectors.toList());
        
        return matchingDrones;
    }
    
    /**
     * Get set of all drone IDs available at service points
     */
    private Set<String> getAvailableDroneIds(List<DroneForServicePoint> dronesForServicePoints) {
        Set<String> droneIds = new HashSet<>();
        if (dronesForServicePoints == null) {
            return droneIds;
        }
        
        for (DroneForServicePoint sp : dronesForServicePoints) {
            if (sp != null && sp.getDrones() != null) {
                for (DroneAvailabilityInfo info : sp.getDrones()) {
                    if (info != null && info.getId() != null) {
                        droneIds.add(info.getId());
                    }
                }
            }
        }
        return droneIds;
    }
    
    /**
     * Public method to check if a specific drone can handle a list of dispatches
     * Used by DeliveryPathService for multi-drone assignment
     */
    public boolean canDroneHandleDispatches(
            Drone drone,
            List<MedDispatchRec> dispatches,
            List<DroneForServicePoint> dronesForServicePoints,
            List<ServicePoint> servicePoints) {
        return canHandleAllDispatches(drone, dispatches, dronesForServicePoints, servicePoints);
    }
    
    /**
     * Check if a drone can handle all dispatches
     */
    private boolean canHandleAllDispatches(
            Drone drone,
            List<MedDispatchRec> dispatches,
            List<DroneForServicePoint> dronesForServicePoints,
            List<ServicePoint> servicePoints) {
        
        if (drone == null || drone.getCapability() == null) {
            return false;
        }
        
        // Calculate total capacity required (sum of all dispatches)
        // Per Piazza @120: "it must be the sum (all together)"
        double totalCapacityRequired = dispatches.stream()
                .filter(d -> d != null && d.getRequirements() != null && d.getRequirements().getCapacity() != null)
                .mapToDouble(d -> d.getRequirements().getCapacity())
                .sum();
        
        // Check if drone capacity is sufficient for total
        if (totalCapacityRequired > 0) {
            Double droneCapacity = drone.getCapability().getCapacity();
            if (droneCapacity == null || droneCapacity < totalCapacityRequired) {
                return false;
            }
        }
        
        // Check each dispatch for other requirements (cooling, heating, date/time)
        // Note: maxCost is NOT checked here for multi-delivery routes because:
        // 1. It's an approximation (per Piazza @125, @134)
        // 2. For multiple deliveries, the route cost is shared (one return trip, not per-delivery)
        // 3. The actual cost will be checked in calcDeliveryPath after path calculation
        // For single delivery, maxCost is still checked
        if (dispatches.size() == 1) {
            // Single delivery: check maxCost as part of capability requirements
            return dispatches.stream()
                    .allMatch(dispatch -> canHandleDispatchIgnoringCapacity(drone, dispatch, dronesForServicePoints, servicePoints));
        } else {
            // Multiple deliveries: skip maxCost check (will be checked in calcDeliveryPath)
            return dispatches.stream()
                    .allMatch(dispatch -> canHandleDispatchIgnoringCapacityAndMaxCost(drone, dispatch, dronesForServicePoints, servicePoints));
        }
    }
    
    /**
     * Check if a drone can handle a single dispatch (ignoring capacity, which is checked separately as sum)
     */
    private boolean canHandleDispatchIgnoringCapacity(
            Drone drone,
            MedDispatchRec dispatch,
            List<DroneForServicePoint> dronesForServicePoints,
            List<ServicePoint> servicePoints) {
        
        if (dispatch == null || dispatch.getRequirements() == null) {
            return false;
        }
        
        // Check capability requirements (excluding capacity, including maxCost for single delivery)
        if (!meetsCapabilityRequirementsIgnoringCapacity(drone, dispatch.getRequirements(), dispatch, dronesForServicePoints, servicePoints)) {
            return false;
        }
        
        // Check date/time availability (if provided)
        if (dispatch.getDate() != null && dispatch.getTime() != null) {
            if (!isAvailableAtDateTime(drone, dispatch.getDate(), dispatch.getTime(), dronesForServicePoints)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Check if a drone can handle a single dispatch (ignoring capacity AND maxCost)
     * Used for multiple deliveries where maxCost will be checked after path calculation
     */
    private boolean canHandleDispatchIgnoringCapacityAndMaxCost(
            Drone drone,
            MedDispatchRec dispatch,
            List<DroneForServicePoint> dronesForServicePoints,
            List<ServicePoint> servicePoints) {
        
        if (dispatch == null || dispatch.getRequirements() == null) {
            return false;
        }
        
        // Check capability requirements (excluding capacity AND maxCost)
        if (!meetsCapabilityRequirementsIgnoringCapacityAndMaxCost(drone, dispatch.getRequirements(), dispatch, dronesForServicePoints, servicePoints)) {
            return false;
        }
        
        // Check date/time availability (if provided)
        if (dispatch.getDate() != null && dispatch.getTime() != null) {
            if (!isAvailableAtDateTime(drone, dispatch.getDate(), dispatch.getTime(), dronesForServicePoints)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Check if drone meets capability requirements (excluding capacity, which is checked as sum)
     */
    private boolean meetsCapabilityRequirementsIgnoringCapacity(
            Drone drone, 
            MedDispatchRequirements requirements,
            MedDispatchRec dispatch,
            List<DroneForServicePoint> dronesForServicePoints,
            List<ServicePoint> servicePoints) {
        if (drone == null || drone.getCapability() == null || requirements == null) {
            return false;
        }
        
        DroneCapability cap = drone.getCapability();
        
        // Capacity is checked separately in canHandleAllDispatches as sum
        // Skip capacity check here
        
        // Check cooling 
        if (Boolean.TRUE.equals(requirements.getCooling())) {
            if (!Boolean.TRUE.equals(cap.getCooling())) {
                return false;
            }
        }
        
        // Check heating 
        if (Boolean.TRUE.equals(requirements.getHeating())) {
            if (!Boolean.TRUE.equals(cap.getHeating())) {
                return false;
            }
        }
        
        // Check maxCost (optional) - estimate using Euclidean distance
        // Note: For single delivery, maxCost is checked here
        // For multiple deliveries, maxCost is skipped (checked in calcDeliveryPath)
        if (requirements.getMaxCost() != null) {
            if (!meetsMaxCostRequirement(drone, dispatch, requirements.getMaxCost(), dronesForServicePoints, servicePoints)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Check if drone meets capability requirements (excluding capacity AND maxCost)
     * Used for multiple deliveries where maxCost will be checked after path calculation
     */
    private boolean meetsCapabilityRequirementsIgnoringCapacityAndMaxCost(
            Drone drone, 
            MedDispatchRequirements requirements,
            MedDispatchRec dispatch,
            List<DroneForServicePoint> dronesForServicePoints,
            List<ServicePoint> servicePoints) {
        if (drone == null || drone.getCapability() == null || requirements == null) {
            return false;
        }
        
        DroneCapability cap = drone.getCapability();
        
        // Capacity is checked separately in canHandleAllDispatches as sum
        // Skip capacity check here
        
        // Check cooling 
        if (Boolean.TRUE.equals(requirements.getCooling())) {
            if (!Boolean.TRUE.equals(cap.getCooling())) {
                return false;
            }
        }
        
        // Check heating 
        if (Boolean.TRUE.equals(requirements.getHeating())) {
            if (!Boolean.TRUE.equals(cap.getHeating())) {
                return false;
            }
        }
        
        // Skip maxCost check for multiple deliveries
        // maxCost will be checked in calcDeliveryPath after actual path calculation
        
        return true;
    }
    
    /**
     * Check if a drone can handle a single dispatch
     */
    private boolean canHandleDispatch(
            Drone drone,
            MedDispatchRec dispatch,
            List<DroneForServicePoint> dronesForServicePoints,
            List<ServicePoint> servicePoints) {
        
        if (dispatch == null || dispatch.getRequirements() == null) {
            return false;
        }
        
        // Check capability requirements (including maxCost)
        if (!meetsCapabilityRequirements(drone, dispatch.getRequirements(), dispatch, dronesForServicePoints, servicePoints)) {
            return false;
        }
        
        // Check date/time availability (if provided)
        if (dispatch.getDate() != null && dispatch.getTime() != null) {
            if (!isAvailableAtDateTime(drone, dispatch.getDate(), dispatch.getTime(), dronesForServicePoints)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Check if drone meets capability requirements
     */
    private boolean meetsCapabilityRequirements(
            Drone drone, 
            MedDispatchRequirements requirements,
            MedDispatchRec dispatch,
            List<DroneForServicePoint> dronesForServicePoints,
            List<ServicePoint> servicePoints) {
        if (drone == null || drone.getCapability() == null || requirements == null) {
            return false;
        }
        
        DroneCapability cap = drone.getCapability();
        
        // Check capacity 
        if (requirements.getCapacity() != null) {
            if (cap.getCapacity() == null || cap.getCapacity() < requirements.getCapacity()) {
                return false;
            }
        }
        
        // Check cooling 
        if (Boolean.TRUE.equals(requirements.getCooling())) {
            if (!Boolean.TRUE.equals(cap.getCooling())) {
                return false;
            }
        }
        
        // Check heating 
        if (Boolean.TRUE.equals(requirements.getHeating())) {
            if (!Boolean.TRUE.equals(cap.getHeating())) {
                return false;
            }
        }
        
        // Check maxCost (optional) - estimate using Euclidean distance
        if (requirements.getMaxCost() != null) {
            if (!meetsMaxCostRequirement(drone, dispatch, requirements.getMaxCost(), dronesForServicePoints, servicePoints)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Check if estimated cost for delivery meets maxCost requirement
     * Uses Euclidean distance estimate 
     */
    private boolean meetsMaxCostRequirement(
            Drone drone,
            MedDispatchRec dispatch,
            Double maxCost,
            List<DroneForServicePoint> dronesForServicePoints,
            List<ServicePoint> servicePoints) {
        
        if (drone == null || dispatch == null || dispatch.getDelivery() == null || maxCost == null) {
            return false;
        }
        
        // Find service point for this drone
        ServicePoint servicePoint = findServicePointForDrone(drone.getId(), servicePoints, dronesForServicePoints);
        if (servicePoint == null || servicePoint.getLocation() == null) {
            return false; // Cannot estimate without service point
        }
        
        // Get start and end locations
        LngLat start = new LngLat(servicePoint.getLocation().getLng(), servicePoint.getLocation().getLat());
        LngLat end = dispatch.getDelivery();
        
        // Estimate distance (Euclidean)
        double distance = distanceService.calculateDistance(start, end);
        
        // Estimate moves: distance / step_size (round up)
        // Step size is 0.00015 (from PathfindingService)
        double stepSize = 0.00015;
        int estimatedMoves = (int) Math.ceil(distance / stepSize);
        
        // Add 1 for hover at delivery point
        estimatedMoves += 1;
        
        // Add 1 for return path (estimate same distance back)
        estimatedMoves += (int) Math.ceil(distance / stepSize);
        
        // Calculate estimated cost
        DroneCapability cap = drone.getCapability();
        double costInitial = cap.getCostInitial() != null ? cap.getCostInitial() : 0.0;
        double costPerMove = cap.getCostPerMove() != null ? cap.getCostPerMove() : 0.0;
        double costFinal = cap.getCostFinal() != null ? cap.getCostFinal() : 0.0;
        
        double estimatedCost = costInitial + (costPerMove * estimatedMoves) + costFinal;
        
        // Check if estimated cost is within maxCost
        return estimatedCost <= maxCost;
    }
    
    /**
     * Find service point for a drone
     */
    private ServicePoint findServicePointForDrone(
            String droneId,
            List<ServicePoint> servicePoints,
            List<DroneForServicePoint> dronesForServicePoints) {
        
        if (dronesForServicePoints == null || servicePoints == null || droneId == null) {
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
     * Check if drone is available at specific date and time
     * date is LocalDate, time is LocalTime
     */
    private boolean isAvailableAtDateTime(
            Drone drone,
            String dateStr,
            String timeStr,
            List<DroneForServicePoint> dronesForServicePoints) {
        
        try {
            // Parse date to get day of week
            LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
            DayOfWeek dayOfWeek = date.getDayOfWeek();
            String dayOfWeekStr = dayOfWeek.name(); // MONDAY, TUESDAY, etc.
            
            // Parse time
            LocalTime time = parseTime(timeStr);
            if (time == null) {
                return false;
            }
            
            // Find drone availability info
            List<DroneAvailability> availability = getDroneAvailability(drone.getId(), dronesForServicePoints);
            if (availability == null || availability.isEmpty()) {
                return false; // No availability info = not available
            }
            
            // Check if any availability slot matches
            return availability.stream()
                    .anyMatch(slot -> isTimeInSlot(slot, dayOfWeekStr, time));
            
        } catch (Exception e) {
            // Invalid date/time format
            return false;
        }
    }
    
    /**
     * Parse time string to LocalTime
     * Handles both "14:30" and "14:30:00" formats
     */
    private LocalTime parseTime(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) {
            return null;
        }
        
        try {
            // Try with seconds first
            if (timeStr.length() >= 8) {
                return LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("HH:mm:ss"));
            } else {
                // Without seconds
                return LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("HH:mm"));
            }
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Get availability slots for a drone
     */
    private List<DroneAvailability> getDroneAvailability(
            String droneId,
            List<DroneForServicePoint> dronesForServicePoints) {
        
        if (dronesForServicePoints == null || droneId == null) {
            return null;
        }
        
        for (DroneForServicePoint sp : dronesForServicePoints) {
            if (sp != null && sp.getDrones() != null) {
                for (DroneAvailabilityInfo info : sp.getDrones()) {
                    if (info != null && droneId.equals(info.getId())) {
                        return info.getAvailability();
                    }
                }
            }
        }
        
        return null;
    }
    
    /**
     * Check if time falls within availability slot
     */
    private boolean isTimeInSlot(DroneAvailability slot, String dayOfWeek, LocalTime time) {
        if (slot == null || slot.getDayOfWeek() == null) {
            return false;
        }
        
        // Check day of week matches
        if (!dayOfWeek.equalsIgnoreCase(slot.getDayOfWeek())) {
            return false;
        }
        
        // Parse slot times
        LocalTime fromTime = parseTime(slot.getFrom());
        LocalTime untilTime = parseTime(slot.getUntil());
        
        if (fromTime == null || untilTime == null) {
            return false;
        }
        
        // Check if time is within slot
        // from <= time <= until (both inclusive)
        return !time.isBefore(fromTime) && !time.isAfter(untilTime);
    }
}

