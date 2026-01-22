package uk.ac.ed.ilp.unit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.ac.ed.ilp.model.*;
import uk.ac.ed.ilp.service.DistanceService;
import uk.ac.ed.ilp.service.DroneAvailabilityService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DroneAvailabilityServiceTest {

    private final DroneAvailabilityService service = new DroneAvailabilityService(new DistanceService());

    private Drone drone(String id, Double capacity, Integer maxMoves, Boolean cooling, Boolean heating) {
        Drone d = new Drone();
        d.setId(id);
        DroneCapability cap = new DroneCapability();
        cap.setCapacity(capacity);
        cap.setMaxMoves(maxMoves);
        cap.setCooling(cooling);
        cap.setHeating(heating);
        d.setCapability(cap);
        return d;
    }

    private DroneForServicePoint dfsp(int spId, String... droneIds) {
        DroneForServicePoint dfsp = new DroneForServicePoint();
        dfsp.setServicePointId(spId);
        dfsp.setDrones(List.of(droneAvailabilityInfos(droneIds)));
        return dfsp;
    }

    private DroneAvailabilityInfo[] droneAvailabilityInfos(String... ids) {
        return java.util.Arrays.stream(ids).map(id -> {
            DroneAvailabilityInfo info = new DroneAvailabilityInfo();
            info.setId(id);
            return info;
        }).toArray(DroneAvailabilityInfo[]::new);
    }

    private MedDispatchRec dispatch(double payloadKg, int servicePointId) {
        MedDispatchRec d = new MedDispatchRec();
        MedDispatchRequirements req = new MedDispatchRequirements();
        req.setCapacity(payloadKg);
        d.setRequirements(req);
        LngLat delivery = new LngLat(0.0, 0.0);
        d.setDelivery(delivery);
        return d;
    }

    @Test
    @DisplayName("findAvailableDrones: filters by service point availability and capability")
    void findAvailableDrones_filters() {
        List<Drone> drones = List.of(
                drone("a", 5.0, 1000, true, false),
                drone("b", 2.0, 500, false, false)
        );
        List<DroneForServicePoint> dfsp = List.of(dfsp(1, "a"));
        ServicePoint sp = new ServicePoint();
        sp.setId(1);
        sp.setName("sp1");
        LngLatAlt loc = new LngLatAlt();
        loc.setLng(0.0);
        loc.setLat(0.0);
        loc.setAlt(0);
        sp.setLocation(loc);
        List<ServicePoint> sps = List.of(sp);
        List<MedDispatchRec> dispatches = List.of(dispatch(3.0, 1));

        List<String> result = service.findAvailableDrones(dispatches, drones, dfsp, sps);
        assertThat(result).containsExactly("a");
    }

    @Test
    @DisplayName("canDroneHandleDispatches: enforces max payload and max moves")
    void canDroneHandleDispatches_limits() {
        Drone limited = drone("x", 1.0, 1, false, false);
        List<DroneForServicePoint> dfsp = List.of(dfsp(1, "x"));
        ServicePoint sp = new ServicePoint();
        sp.setId(1);
        sp.setName("sp1");
        LngLatAlt loc = new LngLatAlt();
        loc.setLng(0.0);
        loc.setLat(0.0);
        loc.setAlt(0);
        sp.setLocation(loc);
        List<ServicePoint> sps = List.of(sp);
        List<MedDispatchRec> dispatches = List.of(dispatch(2.0, 1));

        boolean canHandle = service.canDroneHandleDispatches(limited, dispatches, dfsp, sps);
        assertThat(canHandle).isFalse();
    }

    @Test
    @DisplayName("findAvailableDrones: returns empty for null dispatches")
    void findAvailableDrones_nullDispatches() {
        List<String> result = service.findAvailableDrones(null, List.of(), List.of(), List.of());
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findAvailableDrones: returns empty for empty dispatches")
    void findAvailableDrones_emptyDispatches() {
        List<String> result = service.findAvailableDrones(List.of(), List.of(), List.of(), List.of());
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findAvailableDrones: filters by date/time availability")
    void findAvailableDrones_filtersByDateTime() {
        Drone drone = drone("d1", 5.0, 1000, true, false);
        List<Drone> drones = List.of(drone);
        
        DroneAvailabilityInfo dai = new DroneAvailabilityInfo();
        dai.setId("d1");
        DroneAvailability avail = new DroneAvailability();
        avail.setDayOfWeek("MONDAY");
        avail.setFrom("09:00:00");
        avail.setUntil("17:00:00");
        dai.setAvailability(List.of(avail));
        
        DroneForServicePoint dfsp = new DroneForServicePoint();
        dfsp.setServicePointId(1);
        dfsp.setDrones(List.of(dai));
        
        ServicePoint sp = new ServicePoint();
        sp.setId(1);
        LngLatAlt loc = new LngLatAlt();
        loc.setLng(0.0); loc.setLat(0.0); loc.setAlt(0);
        sp.setLocation(loc);
        
        MedDispatchRec dispatch = new MedDispatchRec();
        dispatch.setDate("2025-01-06"); // Monday
        dispatch.setTime("10:00");
        MedDispatchRequirements req = new MedDispatchRequirements();
        req.setCapacity(3.0);
        dispatch.setRequirements(req);
        dispatch.setDelivery(new LngLat(0.0, 0.0));
        
        List<String> result = service.findAvailableDrones(
                List.of(dispatch), drones, List.of(dfsp), List.of(sp));
        assertThat(result).contains("d1");
    }
}
