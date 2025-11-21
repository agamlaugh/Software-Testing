package uk.ac.ed.ilp.service;

import org.springframework.stereotype.Service;
import uk.ac.ed.ilp.model.Drone;
import uk.ac.ed.ilp.model.DroneCapability;
import uk.ac.ed.ilp.model.requests.QueryCondition;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for querying drones by attributes
 * Handles attribute matching and type conversion
 */
@Service
public class DroneQueryService {

    /**
     * Query drones by a single attribute with equals operator
     * 
     * @param drones List of drones to query
     * @param attribute Attribute name to match
     * @param value Value to match (as string)
     * @return List of drone IDs that match
     */
    public List<String> queryByAttribute(List<Drone> drones, String attribute, String value) {
        return drones.stream()
                .filter(drone -> drone != null && drone.getId() != null)
                .filter(drone -> matchesAttribute(drone, attribute, value))
                .map(Drone::getId)
                .collect(Collectors.toList());
    }

    /**
     * Query drones by multiple conditions with operators (AND logic)
     * All conditions must match for a drone to be included
     * 
     * @param drones List of drones to query
     * @param conditions List of query conditions
     * @return List of drone IDs that match all conditions
     */
    public List<String> queryByConditions(List<Drone> drones, List<QueryCondition> conditions) {
        if (conditions == null || conditions.isEmpty()) {
            return List.of(); // Empty list if no conditions
        }
        
        return drones.stream()
                .filter(drone -> drone != null && drone.getId() != null)
                .filter(drone -> matchesAllConditions(drone, conditions))
                .map(Drone::getId)
                .collect(Collectors.toList());
    }

    /**
     * Check if drone matches all conditions (AND logic)
     */
    private boolean matchesAllConditions(Drone drone, List<QueryCondition> conditions) {
        return conditions.stream()
                .allMatch(condition -> matchesCondition(drone, condition));
    }

    /**
     * Check if drone matches a single condition with operator
     */
    private boolean matchesCondition(Drone drone, QueryCondition condition) {
        if (condition == null || condition.getAttribute() == null || 
            condition.getOperator() == null || condition.getValue() == null) {
            return false;
        }
        
        String attribute = condition.getAttribute();
        String operator = condition.getOperator();
        String value = condition.getValue();
        
        // For equals operator, use existing method
        if ("=".equals(operator)) {
            return matchesAttribute(drone, attribute, value);
        }
        
        // For other operators, need special handling
        return matchesAttributeWithOperator(drone, attribute, operator, value);
    }

    /**
     * Check if drone matches attribute with operator (!=, <, >)
     * Only numerical attributes support !=, <, >
     * Boolean/string attributes only support =
     */
    private boolean matchesAttributeWithOperator(Drone drone, String attribute, String operator, String value) {
        // Top-level attributes (id, name) - only support =
        if ("id".equalsIgnoreCase(attribute) || "name".equalsIgnoreCase(attribute)) {
            if ("=".equals(operator)) {
                return matchesAttribute(drone, attribute, value);
            }
            return false; // Only = supported for string attributes
        }
        
        // Capability attributes
        if (drone.getCapability() == null) {
            return false;
        }
        
        DroneCapability cap = drone.getCapability();
        
        // Boolean attributes (cooling, heating) - only support =
        if ("cooling".equalsIgnoreCase(attribute) || "heating".equalsIgnoreCase(attribute)) {
            if ("=".equals(operator)) {
                return matchesAttribute(drone, attribute, value);
            }
            return false; // Only = supported for boolean attributes
        }
        
        // Numeric attributes support =, !=, <, >
        return matchesNumericAttribute(cap, attribute, operator, value);
    }

    /**
     * Check numeric attribute with operator
     */
    private boolean matchesNumericAttribute(DroneCapability cap, String attribute, String operator, String value) {
        try {
            // Handle Double attributes
            if ("capacity".equalsIgnoreCase(attribute)) {
                Double capacity = cap.getCapacity();
                if (capacity == null) return false;
                double valueDouble = Double.parseDouble(value);
                return compareNumeric(capacity, operator, valueDouble);
            }
            if ("costPerMove".equalsIgnoreCase(attribute)) {
                Double costPerMove = cap.getCostPerMove();
                if (costPerMove == null) return false;
                double valueDouble = Double.parseDouble(value);
                return compareNumeric(costPerMove, operator, valueDouble);
            }
            if ("costInitial".equalsIgnoreCase(attribute)) {
                Double costInitial = cap.getCostInitial();
                if (costInitial == null) return false;
                double valueDouble = Double.parseDouble(value);
                return compareNumeric(costInitial, operator, valueDouble);
            }
            if ("costFinal".equalsIgnoreCase(attribute)) {
                Double costFinal = cap.getCostFinal();
                if (costFinal == null) return false;
                double valueDouble = Double.parseDouble(value);
                return compareNumeric(costFinal, operator, valueDouble);
            }
            
            // Handle Integer attributes
            if ("maxMoves".equalsIgnoreCase(attribute)) {
                Integer maxMoves = cap.getMaxMoves();
                if (maxMoves == null) return false;
                int valueInt = Integer.parseInt(value);
                return compareNumeric(maxMoves.doubleValue(), operator, valueInt);
            }
        } catch (NumberFormatException e) {
            return false; // Invalid number format
        }
        
        return false; // Unknown attribute
    }

    /**
     * Compare numeric values with operator
     */
    private boolean compareNumeric(double actual, String operator, double expected) {
        switch (operator) {
            case "=":
                return Math.abs(actual - expected) < 0.0001; // Handle floating point
            case "!=":
                return Math.abs(actual - expected) >= 0.0001;
            case "<":
                return actual < expected;
            case ">":
                return actual > expected;
            default:
                return false;
        }
    }

    /**
     * Check if drone matches attribute=value
     * Handles both top-level attributes (id, name) and nested capability attributes
     */
    private boolean matchesAttribute(Drone drone, String attribute, String value) {
        // Top-level attributes
        if ("id".equalsIgnoreCase(attribute)) {
            return value.equals(drone.getId());
        }
        if ("name".equalsIgnoreCase(attribute)) {
            return drone.getName() != null && value.equals(drone.getName());
        }
        
        // Capability attributes (need to handle type conversion)
        if (drone.getCapability() == null) {
            return false;
        }
        
        DroneCapability cap = drone.getCapability();
        
        // Boolean attributes
        if ("cooling".equalsIgnoreCase(attribute)) {
            Boolean cooling = cap.getCooling();
            boolean hasCooling = Boolean.TRUE.equals(cooling);
            boolean valueBool = Boolean.parseBoolean(value);
            return hasCooling == valueBool;
        }
        if ("heating".equalsIgnoreCase(attribute)) {
            Boolean heating = cap.getHeating();
            boolean hasHeating = Boolean.TRUE.equals(heating);
            boolean valueBool = Boolean.parseBoolean(value);
            return hasHeating == valueBool;
        }
        
        // Numeric attributes - need to parse and compare
        if ("capacity".equalsIgnoreCase(attribute)) {
            Double capacity = cap.getCapacity();
            if (capacity == null) return false;
            try {
                double valueDouble = Double.parseDouble(value);
                return Math.abs(capacity - valueDouble) < 0.0001; // Handle floating point comparison
            } catch (NumberFormatException e) {
                return false;
            }
        }
        if ("maxMoves".equalsIgnoreCase(attribute)) {
            Integer maxMoves = cap.getMaxMoves();
            if (maxMoves == null) return false;
            try {
                int valueInt = Integer.parseInt(value);
                return maxMoves.equals(valueInt);
            } catch (NumberFormatException e) {
                return false;
            }
        }
        if ("costPerMove".equalsIgnoreCase(attribute)) {
            Double costPerMove = cap.getCostPerMove();
            if (costPerMove == null) return false;
            try {
                double valueDouble = Double.parseDouble(value);
                return Math.abs(costPerMove - valueDouble) < 0.0001;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        if ("costInitial".equalsIgnoreCase(attribute)) {
            Double costInitial = cap.getCostInitial();
            if (costInitial == null) return false;
            try {
                double valueDouble = Double.parseDouble(value);
                return Math.abs(costInitial - valueDouble) < 0.0001;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        if ("costFinal".equalsIgnoreCase(attribute)) {
            Double costFinal = cap.getCostFinal();
            if (costFinal == null) return false;
            try {
                double valueDouble = Double.parseDouble(value);
                return Math.abs(costFinal - valueDouble) < 0.0001;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        
        // Unknown attribute
        return false;
    }
}

