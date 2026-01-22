package uk.ac.ed.ilp.unit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.ac.ed.ilp.model.Drone;
import uk.ac.ed.ilp.model.DroneCapability;
import uk.ac.ed.ilp.model.requests.QueryCondition;
import uk.ac.ed.ilp.service.DroneQueryService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DroneQueryServiceTest {

    private final DroneQueryService service = new DroneQueryService();

    private Drone drone(String id, Double capacity, Boolean cooling, Boolean heating, Double costPerMove) {
        Drone d = new Drone();
        d.setId(id);
        DroneCapability cap = new DroneCapability();
        cap.setCapacity(capacity);
        cap.setCooling(cooling);
        cap.setHeating(heating);
        cap.setCostPerMove(costPerMove);
        d.setCapability(cap);
        return d;
    }

    @Test
    @DisplayName("queryByAttribute: matches by capability capacity and boolean flags")
    void queryByAttribute_matches() {
        List<Drone> drones = List.of(
                drone("1", 10.0, true, false, 1.0),
                drone("2", 5.0, false, true, 0.5)
        );

        assertThat(service.queryByAttribute(drones, "capacity", "10.0")).containsExactly("1");
        assertThat(service.queryByAttribute(drones, "cooling", "true")).containsExactly("1");
        assertThat(service.queryByAttribute(drones, "heating", "true")).containsExactly("2");
    }

    @Test
    @DisplayName("queryByConditions: AND logic across multiple conditions")
    void queryByConditions_andLogic() {
        List<Drone> drones = List.of(
                drone("1", 10.0, true, false, 1.0),
                drone("2", 8.0, true, false, 0.8),
                drone("3", 4.0, false, false, 0.4)
        );

        QueryCondition condCapacity = new QueryCondition();
        condCapacity.setAttribute("capacity");
        condCapacity.setOperator(">");
        condCapacity.setValue("9");

        QueryCondition condCooling = new QueryCondition();
        condCooling.setAttribute("cooling");
        condCooling.setOperator("=");
        condCooling.setValue("true");

        List<String> result = service.queryByConditions(drones, List.of(condCapacity, condCooling));
        assertThat(result).containsExactly("1");
    }
}
