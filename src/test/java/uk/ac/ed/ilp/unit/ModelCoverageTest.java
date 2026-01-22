package uk.ac.ed.ilp.unit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.ac.ed.ilp.model.*;
import uk.ac.ed.ilp.model.requests.QueryCondition;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ModelCoverageTest {

    @Test
    @DisplayName("Models: getters/setters round-trip basic fields")
    void models_roundTrip() {
        // Drone + capability
        DroneCapability cap = new DroneCapability();
        cap.setCapacity(5.0);
        cap.setCooling(true);
        cap.setHeating(false);
        cap.setMaxMoves(100);
        cap.setCostPerMove(1.1);
        cap.setCostInitial(0.2);
        cap.setCostFinal(0.3);

        Drone drone = new Drone();
        drone.setId("d1");
        drone.setName("alpha");
        drone.setCapability(cap);
        assertThat(drone.getCapability().getCapacity()).isEqualTo(5.0);

        // Med dispatch
        MedDispatchRequirements req = new MedDispatchRequirements();
        req.setCapacity(2.0);
        req.setCooling(true);
        req.setHeating(false);
        req.setMaxCost(50.0);

        MedDispatchRec dispatch = new MedDispatchRec();
        dispatch.setId(1);
        dispatch.setDate("2025-01-01");
        dispatch.setTime("12:00");
        dispatch.setRequirements(req);
        dispatch.setDelivery(new LngLat(0.1, 0.2));
        assertThat(dispatch.getRequirements().getCapacity()).isEqualTo(2.0);

        // ServicePoint + location
        ServicePoint sp = new ServicePoint();
        sp.setId(10);
        sp.setName("sp");
        LngLatAlt loc = new LngLatAlt();
        loc.setLng(0.0);
        loc.setLat(0.0);
        loc.setAlt(0);
        sp.setLocation(loc);
        assertThat(sp.getLocation().getLng()).isEqualTo(0.0);

        // RestrictedArea
        RestrictedArea ra = new RestrictedArea();
        ra.setName("r1");
        ra.setId(5);
        ra.setVertices(List.of(new LngLat(0.0, 0.0)));
        assertThat(ra.getVertices()).hasSize(1);

        // Availability models
        DroneAvailabilityInfo info = new DroneAvailabilityInfo();
        info.setId("d1");
        DroneAvailability availability = new DroneAvailability();
        availability.setDayOfWeek("MONDAY");
        availability.setFrom("09:00:00");
        availability.setUntil("17:00:00");
        info.setAvailability(List.of(availability));
        assertThat(info.getAvailability().getFirst().getDayOfWeek()).isEqualTo("MONDAY");

        DroneForServicePoint dfsp = new DroneForServicePoint();
        dfsp.setServicePointId(10);
        dfsp.setDrones(List.of(info));
        assertThat(dfsp.getDrones()).hasSize(1);

        // DeliveryPathResponse + DronePath
        DeliveryPath deliveryPath = new DeliveryPath();
        deliveryPath.setDeliveryId(1);
        deliveryPath.setFlightPath(List.of(new LngLat(0.0, 0.0), new LngLat(0.1, 0.1)));

        DronePath dp = new DronePath();
        dp.setDroneId("d1");
        dp.setDeliveries(List.of(deliveryPath));

        DeliveryPathResponse resp = new DeliveryPathResponse();
        resp.setTotalCost(12.3);
        resp.setTotalMoves(5);
        resp.setDronePaths(List.of(dp));
        assertThat(resp.getDronePaths().getFirst().getDroneId()).isEqualTo("d1");

        // QueryCondition
        QueryCondition qc = new QueryCondition();
        qc.setAttribute("capacity");
        qc.setOperator(">");
        qc.setValue("5");
        assertThat(qc.getOperator()).isEqualTo(">");

        // LngLatAlt
        LngLatAlt alt = new LngLatAlt();
        alt.setLng(1.0);
        alt.setLat(2.0);
        alt.setAlt(100);
        assertThat(alt.getAlt()).isEqualTo(100);

        // ComparisonStats
        ComparisonStats stats = ComparisonStats.builder()
                .singleDronePossible(true)
                .recommendation("SINGLE")
                .reason("test")
                .singleDroneCost(10.0)
                .multiDroneCost(15.0)
                .singleDroneMoves(5)
                .multiDroneMoves(8)
                .build();
        assertThat(stats.getRecommendation()).isEqualTo("SINGLE");

        // RouteComparisonResponse
        RouteComparisonResponse comp = RouteComparisonResponse.builder()
                .singleDroneSolution(resp)
                .multiDroneSolution(resp)
                .comparison(stats)
                .build();
        assertThat(comp.getComparison()).isNotNull();

        // GeoJsonFeatureCollection
        GeoJsonFeatureCollection geoJson = new GeoJsonFeatureCollection();
        geoJson.setType("FeatureCollection");
        geoJson.setFeatures(List.of());
        assertThat(geoJson.getType()).isEqualTo("FeatureCollection");

        // GeoJsonFeature
        GeoJsonFeature feature = new GeoJsonFeature();
        feature.setType("Feature");
        assertThat(feature.getType()).isEqualTo("Feature");

        // GeoJsonGeometry
        GeoJsonGeometry geom = new GeoJsonGeometry();
        geom.setType("LineString");
        geom.setCoordinates(List.of());
        assertThat(geom.getType()).isEqualTo("LineString");

        // GeoJsonProperties
        GeoJsonProperties props = new GeoJsonProperties();
        props.setDroneId("d1");
        assertThat(props.getDroneId()).isEqualTo("d1");
    }
}
