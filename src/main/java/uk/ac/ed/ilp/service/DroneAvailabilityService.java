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
     * Returns a result object with details about why it failed (for debugging)
     */
    private static class CanHandleResult {
        boolean canHandle;
        String failureReason;
        
        CanHandleResult(boolean canHandle, String failureReason) {
            this.canHandle = canHandle;
            this.failureReason = failureReason;
        }
        
        static CanHandleResult success() {
            return new CanHandleResult(true, null);
        }
        
        static CanHandleResult failure(String reason) {
            return new CanHandleResult(false, reason);
        }
    }
    
    /**
     * Check if a drone can handle all dispatches (with detailed failure reasons)
     */
    private CanHandleResult canHandleAllDispatchesDetailed(
            Drone drone,
            List<MedDispatchRec> dispatches,
            List<DroneForServicePoint> dronesForServicePoints,
            List<ServicePoint> servicePoints) {
        
        if (drone == null || drone.getCapability() == null) {
            return CanHandleResult.failure("drone or capability is null");
        }
        
        // Calculate total capacity required (sum of all dispatches)
        // Per spec: "A drone must satisfy ALL dispatches" - check total capacity
        double totalCapacityRequired = dispatches.stream()
                .filter(d -> d != null && d.getRequirements() != null && d.getRequirements().getCapacity() != null)
                .mapToDouble(d -> d.getRequirements().getCapacity())
                .sum();
        
        // Check if drone capacity is sufficient for total
        if (totalCapacityRequired > 0) {
            Double droneCapacity = drone.getCapability().getCapacity();
            if (droneCapacity == null || droneCapacity < totalCapacityRequired) {
                return CanHandleResult.failure("capacity: required " + totalCapacityRequired + " > drone capacity " + droneCapacity);
            }
        }
        
        // Check each dispatch for other requirements (cooling, heating, date/time)
        if (dispatches.size() == 1) {
            MedDispatchRec dispatch = dispatches.get(0);
            if (!canHandleDispatchIgnoringCapacity(drone, dispatch, dronesForServicePoints, servicePoints)) {
                MedDispatchRequirements req = dispatch.getRequirements();
                String reason = "capability/availability check failed";
                if (req != null) {
                    if (Boolean.TRUE.equals(req.getHeating()) && !Boolean.TRUE.equals(drone.getCapability().getHeating())) {
                        reason = "heating required but drone doesn't have it";
                    } else if (Boolean.TRUE.equals(req.getCooling()) && !Boolean.TRUE.equals(drone.getCapability().getCooling())) {
                        reason = "cooling required but drone doesn't have it";
                    } else if (dispatch.getDate() != null && dispatch.getTime() != null) {
                        reason = "not available at date/time " + dispatch.getDate() + " " + dispatch.getTime();
                    }
                }
                return CanHandleResult.failure(reason);
            }
        } else {
            // Multiple deliveries: skip maxCost check (will be checked in calcDeliveryPath)
            // CRITICAL: Check if dispatches have conflicting heating/cooling requirements
            // A drone cannot simultaneously provide heating for one delivery and cooling for another
            boolean hasHeatingRequirement = dispatches.stream()
                    .anyMatch(d -> d.getRequirements() != null && Boolean.TRUE.equals(d.getRequirements().getHeating()));
            boolean hasCoolingRequirement = dispatches.stream()
                    .anyMatch(d -> d.getRequirements() != null && Boolean.TRUE.equals(d.getRequirements().getCooling()));
            
            // If we have both heating and cooling requirements, the drone cannot handle both
            // Per spec: heating and cooling are mutually exclusive per delivery.
            // If one delivery needs heating and another needs cooling, they cannot be on the same drone
            // because a drone can only provide one type of temperature control at a time.
            if (hasHeatingRequirement && hasCoolingRequirement) {
                // Reject this combination - cannot provide heating for one delivery and cooling for another simultaneously
                return CanHandleResult.failure("cannot handle both heating and cooling requirements simultaneously (one delivery needs heating, another needs cooling)");
            }
            
            // Now check each dispatch individually
            for (MedDispatchRec dispatch : dispatches) {
                if (!canHandleDispatchIgnoringCapacityAndMaxCost(drone, dispatch, dronesForServicePoints, servicePoints)) {
                    MedDispatchRequirements req = dispatch.getRequirements();
                    String reason = "capability/availability check failed for delivery " + dispatch.getId();
                    if (req != null && drone.getCapability() != null) {
                        // Check heating first
                        if (Boolean.TRUE.equals(req.getHeating()) && !Boolean.TRUE.equals(drone.getCapability().getHeating())) {
                            reason = "delivery " + dispatch.getId() + " requires heating but drone doesn't have it";
                        } 
                        // Check cooling
                        else if (Boolean.TRUE.equals(req.getCooling()) && !Boolean.TRUE.equals(drone.getCapability().getCooling())) {
                            reason = "delivery " + dispatch.getId() + " requires cooling but drone doesn't have it";
                        } 
                        // Check date/time (but verify it's actually the issue)
                        else if (dispatch.getDate() != null && dispatch.getTime() != null) {
                            // Double-check availability to ensure this is the real issue
                            boolean actuallyAvailable = isAvailableAtDateTime(drone, dispatch.getDate(), dispatch.getTime(), dronesForServicePoints);
                            if (!actuallyAvailable) {
                                reason = "drone not available at date/time " + dispatch.getDate() + " " + dispatch.getTime() + " for delivery " + dispatch.getId();
                            } else {
                                // Date/time is fine, so must be something else - check if capability is null
                                if (drone.getCapability() == null) {
                                    reason = "drone " + drone.getId() + " has null capability";
                                } else {
                                    reason = "unknown capability/availability issue for delivery " + dispatch.getId() + " (date/time is available)";
                                }
                            }
                        }
                    }
                    return CanHandleResult.failure(reason);
                }
            }
        }
        
        return CanHandleResult.success();
    }
    
    /**
     * Check if a drone can handle all dispatches
     */
    private boolean canHandleAllDispatches(
            Drone drone,
            List<MedDispatchRec> dispatches,
            List<DroneForServicePoint> dronesForServicePoints,
            List<ServicePoint> servicePoints) {
        return canHandleAllDispatchesDetailed(drone, dispatches, dronesForServicePoints, servicePoints).canHandle;
    }
    
    /**
     * Public method to check if a drone can handle dispatches with detailed failure reason
     */
    public String canDroneHandleDispatchesWithReason(
            Drone drone,
            List<MedDispatchRec> dispatches,
            List<DroneForServicePoint> dronesForServicePoints,
            List<ServicePoint> servicePoints) {
        CanHandleResult result = canHandleAllDispatchesDetailed(drone, dispatches, dronesForServicePoints, servicePoints);
        return result.failureReason;
    }
    
    /**
     * Check if a drone can handle a single dispatch (ignoring capacity, which is checked separately per delivery)
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
        boolean meetsCapabilities = meetsCapabilityRequirementsIgnoringCapacityAndMaxCost(drone, dispatch.getRequirements(), dispatch, dronesForServicePoints, servicePoints);
        if (!meetsCapabilities) {
            return false;
        }
        
        // Check date/time availability (if provided)
        if (dispatch.getDate() != null && dispatch.getTime() != null) {
            boolean isAvailable = isAvailableAtDateTime(drone, dispatch.getDate(), dispatch.getTime(), dronesForServicePoints);
            if (!isAvailable) {
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
        
        // Check maxCost estimate using Euclidean distance
        // For single delivery, maxCost is checked here
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
     * Public method to check if a drone is available at a specific date and time
     * Used by DeliveryPathService for multi-drone assignment
     */
    public boolean isDroneAvailableAtDateTime(
            Drone drone,
            String dateStr,
            String timeStr,
            List<DroneForServicePoint> dronesForServicePoints) {
        return isAvailableAtDateTime(drone, dateStr, timeStr, dronesForServicePoints);
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
        
        // Check day of week matches (case-insensitive)
        String slotDay = slot.getDayOfWeek();
        if (!dayOfWeek.equalsIgnoreCase(slotDay)) {
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

