package uk.ac.ed.ilp.unit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.ac.ed.ilp.model.LngLat;
import uk.ac.ed.ilp.service.RegionService;

import static org.assertj.core.api.Assertions.assertThat;

class RegionServiceTest {

    private final RegionService service = new RegionService();

    @Test
    @DisplayName("hasRegion: returns true for known region")
    void hasRegion_known() {
        assertThat(service.hasRegion("sample")).isTrue();
    }

    @Test
    @DisplayName("hasRegion: returns false for unknown region")
    void hasRegion_unknown() {
        assertThat(service.hasRegion("unknown")).isFalse();
    }

    @Test
    @DisplayName("regionNames: returns set of region names")
    void regionNames_returnsSet() {
        assertThat(service.regionNames()).contains("sample");
    }

    @Test
    @DisplayName("contains: returns true for point inside region")
    void contains_inside() {
        LngLat point = new LngLat(-3.19, 55.94);
        assertThat(service.contains("sample", point)).isTrue();
    }

    @Test
    @DisplayName("contains: returns false for point outside region")
    void contains_outside() {
        LngLat point = new LngLat(0.0, 0.0);
        assertThat(service.contains("sample", point)).isFalse();
    }

    @Test
    @DisplayName("contains: returns false for null point")
    void contains_nullPoint() {
        assertThat(service.contains("sample", null)).isFalse();
    }

    @Test
    @DisplayName("contains: returns false for invalid point")
    void contains_invalidPoint() {
        LngLat invalid = new LngLat(200.0, 0.0);
        assertThat(service.contains("sample", invalid)).isFalse();
    }

    @Test
    @DisplayName("contains: returns false for unknown region")
    void contains_unknownRegion() {
        LngLat point = new LngLat(0.0, 0.0);
        assertThat(service.contains("unknown", point)).isFalse();
    }
}
