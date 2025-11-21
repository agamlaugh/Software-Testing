package uk.ac.ed.ilp.service;

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

    /**
     * Find drones that can handle all dispatches
     * All dispatches must be matchable by ONE drone (AND logic)
     * 
     * @param dispatches List of medical dispatch records
     * @param allDrones All available drones
     * @param dronesForServicePoints Drone availability at service points
     * @return List of drone IDs that can handle all dispatches
     */
    public List<String> findAvailableDrones(
            List<MedDispatchRec> dispatches,
            List<Drone> allDrones,
            List<DroneForServicePoint> dronesForServicePoints) {
        
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
                .filter(drone -> canHandleAllDispatches(drone, dispatches, dronesForServicePoints))
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
     * Check if a drone can handle all dispatches
     */
    private boolean canHandleAllDispatches(
            Drone drone,
            List<MedDispatchRec> dispatches,
            List<DroneForServicePoint> dronesForServicePoints) {
        
        return dispatches.stream()
                .allMatch(dispatch -> canHandleDispatch(drone, dispatch, dronesForServicePoints));
    }
    
    /**
     * Check if a drone can handle a single dispatch
     */
    private boolean canHandleDispatch(
            Drone drone,
            MedDispatchRec dispatch,
            List<DroneForServicePoint> dronesForServicePoints) {
        
        if (dispatch == null || dispatch.getRequirements() == null) {
            return false;
        }
        
        // Check capability requirements
        if (!meetsCapabilityRequirements(drone, dispatch.getRequirements())) {
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
    private boolean meetsCapabilityRequirements(Drone drone, MedDispatchRequirements requirements) {
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
        
        // Check maxCost (optional)
        // Note: maxCost comparison would need cost calculation, but for now we'll skip it
        // as it's not clear from the spec how to calculate total cost for a dispatch
        
        return true;
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
        // "from" is inclusive, "until" is typically exclusive (but handle edge case where until is 00:00:00 next day)
        if (untilTime.equals(LocalTime.MIDNIGHT)) {
            // If until is midnight, it means until end of day (23:59:59.999...)
            return !time.isBefore(fromTime);
        }
        // Normal case: from <= time < until
        return !time.isBefore(fromTime) && time.isBefore(untilTime);
    }
}

