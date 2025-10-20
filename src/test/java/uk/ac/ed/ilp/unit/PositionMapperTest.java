package uk.ac.ed.ilp.unit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.ac.ed.ilp.mapper.PositionMapper;
import uk.ac.ed.ilp.model.LngLat;
import uk.ac.ed.ilp.model.Region;
import uk.ac.ed.ilp.model.requests.DistanceRequest;
import uk.ac.ed.ilp.model.requests.IsInRegionSpecRequest;
import uk.ac.ed.ilp.model.requests.NextPositionRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PositionMapperTest {

    private final PositionMapper mapper = new PositionMapper();

    @Test
    @DisplayName("Factory methods keep supplied payload intact")
    void dtoFactoriesReuseSuppliedPayload() {
        LngLat position1 = new LngLat(-3.19, 55.94);
        LngLat position2 = new LngLat(-3.18, 55.95);
        LngLat start = new LngLat(-3.19, 55.94);
        Region region = new Region();
        region.setName("central");

        DistanceRequest distance = mapper.createDistanceRequest(position1, position2);
        NextPositionRequest next = mapper.createNextPositionRequest(start, 90.0);
        IsInRegionSpecRequest regionRequest = mapper.createIsInRegionRequest(position1, region);

        assertAll(
                () -> assertThat(distance.getPosition1()).isSameAs(position1),
                () -> assertThat(distance.getPosition2()).isSameAs(position2),
                () -> assertThat(next.getStart()).isSameAs(start),
                () -> assertThat(next.getAngle()).isEqualTo(90.0),
                () -> assertThat(regionRequest.getPosition()).isSameAs(position1),
                () -> assertThat(regionRequest.getRegion()).isSameAs(region)
        );
    }

    @Test
    @DisplayName("Validation guards reject out-of-range coordinates and open polygons")
    void validationGuardsCoordinatesAndRegions() {
        LngLat valid = mapper.createValidLngLat(-3.19, 55.94);
        assertAll(
                () -> assertThat(valid.getLng()).isEqualTo(-3.19),
                () -> assertThat(valid.getLat()).isEqualTo(55.94)
        );
        assertThrows(IllegalArgumentException.class, () -> mapper.createValidLngLat(200.0, 100.0));

        List<LngLat> vertices = List.of(
                new LngLat(-3.20, 55.93),
                new LngLat(-3.18, 55.93),
                new LngLat(-3.18, 55.95),
                new LngLat(-3.20, 55.95),
                new LngLat(-3.20, 55.93)
        );

        Region region = mapper.createValidRegion("central", vertices);
        assertAll(
                () -> assertThat(region.getName()).isEqualTo("central"),
                () -> assertThat(region.getVertices()).containsExactlyElementsOf(vertices)
        );
        assertThrows(IllegalArgumentException.class, () -> mapper.createValidRegion("central", vertices.subList(0, 4)));
    }

    @Test
    @DisplayName("Helper utilities normalise angles and copy coordinates safely")
    void helperUtilities_normaliseAnglesAndCopy() {
        assertAll(
                () -> assertThat(mapper.normalizeAngle(45)).isEqualTo(45),
                () -> assertThat(mapper.normalizeAngle(-45)).isEqualTo(315),
                () -> assertThat(mapper.normalizeAngle(765)).isEqualTo(45)
        );
        LngLat source = new LngLat(-3.19, 55.94);
        LngLat copy = mapper.copyLngLat(source);
        assertAll(
                () -> assertThat(copy).isNotSameAs(source),
                () -> assertThat(copy.getLng()).isEqualTo(source.getLng()),
                () -> assertThat(copy.getLat()).isEqualTo(source.getLat())
        );
        assertThrows(IllegalArgumentException.class, () -> mapper.copyLngLat(null));
    }
}
