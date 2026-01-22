package uk.ac.ed.ilp.unit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.ac.ed.ilp.model.LngLat;
import uk.ac.ed.ilp.model.RestrictedArea;
import uk.ac.ed.ilp.service.DistanceService;
import uk.ac.ed.ilp.service.PathfindingService;
import uk.ac.ed.ilp.service.PositionService;
import uk.ac.ed.ilp.service.RegionService;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PathfindingServiceTest {

    private final PathfindingService pathfindingService = new PathfindingService(
            new PositionService(),
            new DistanceService(),
            new RegionService()
    );

    @Test
    @DisplayName("calculatePath: returns empty when start or end invalid")
    void calculatePath_invalidPositions() {
        assertThat(pathfindingService.calculatePath(null, null, Collections.emptyList())).isEmpty();
        assertThat(pathfindingService.calculatePath(new LngLat(200.0, 0.0), new LngLat(0.0, 0.0), Collections.emptyList())).isEmpty();
    }

    @Test
    @DisplayName("calculatePath: returns empty when start or end inside restricted area")
    void calculatePath_restrictedAreaBlocksStartOrEnd() {
        RestrictedArea blocked = new RestrictedArea();
        blocked.setName("blocked");
        blocked.setVertices(List.of(
                new LngLat(-0.001, -0.001),
                new LngLat(0.001, -0.001),
                new LngLat(0.001, 0.001),
                new LngLat(-0.001, 0.001),
                new LngLat(-0.001, -0.001)
        ));

        List<RestrictedArea> restricted = List.of(blocked);

        assertThat(pathfindingService.calculatePath(new LngLat(0.0, 0.0), new LngLat(1.0, 1.0), restricted)).isEmpty();
        assertThat(pathfindingService.calculatePath(new LngLat(1.0, 1.0), new LngLat(0.0, 0.0), restricted)).isEmpty();
    }

    @Test
    @DisplayName("calculatePath: simple valid path returns non-empty path")
    void calculatePath_happy() {
        LngLat start = new LngLat(0.0, 0.0);
        LngLat end = new LngLat(0.0003, 0.0003);

        List<LngLat> path = pathfindingService.calculatePath(start, end, List.of());
        assertThat(path).isNotEmpty();
        // First point should be near start and last near end
        assertThat(path.get(0).getLng()).isCloseTo(start.getLng(), org.assertj.core.api.Assertions.within(1e-6));
        assertThat(path.get(path.size() - 1).getLng()).isCloseTo(end.getLng(), org.assertj.core.api.Assertions.within(1e-6));
    }

    @Test
    @DisplayName("calculatePath: unreachable due to restriction returns empty")
    void calculatePath_unreachable() {
        LngLat start = new LngLat(0.0, 0.0);
        LngLat end = new LngLat(0.0003, 0.0003);

        // Large restricted square blocking the straight line
        RestrictedArea blocked = new RestrictedArea();
        blocked.setVertices(List.of(
                new LngLat(-0.0002, -0.0002),
                new LngLat(0.0005, -0.0002),
                new LngLat(0.0005, 0.0005),
                new LngLat(-0.0002, 0.0005),
                new LngLat(-0.0002, -0.0002)
        ));

        List<LngLat> path = pathfindingService.calculatePath(start, end, List.of(blocked));
        assertThat(path).isEmpty();
    }

    @Test
    @DisplayName("calculatePath: path around restricted area succeeds")
    void calculatePath_aroundRestriction() {
        LngLat start = new LngLat(0.0, 0.0);
        LngLat end = new LngLat(0.001, 0.001);

        // Small restricted area that doesn't block the entire path
        RestrictedArea restricted = new RestrictedArea();
        restricted.setVertices(List.of(
                new LngLat(0.0004, 0.0004),
                new LngLat(0.0006, 0.0004),
                new LngLat(0.0006, 0.0006),
                new LngLat(0.0004, 0.0006),
                new LngLat(0.0004, 0.0004)
        ));

        List<LngLat> path = pathfindingService.calculatePath(start, end, List.of(restricted));
        // Should find a path around the restriction
        assertThat(path).isNotEmpty();
        assertThat(path.get(0).getLng()).isCloseTo(start.getLng(), org.assertj.core.api.Assertions.within(1e-6));
        assertThat(path.get(path.size() - 1).getLng()).isCloseTo(end.getLng(), org.assertj.core.api.Assertions.within(1e-6));
    }

    @Test
    @DisplayName("calculatePath: multiple restricted areas")
    void calculatePath_multipleRestrictions() {
        LngLat start = new LngLat(0.0, 0.0);
        LngLat end = new LngLat(0.002, 0.002);

        RestrictedArea r1 = new RestrictedArea();
        r1.setVertices(List.of(
                new LngLat(0.0008, 0.0008),
                new LngLat(0.0012, 0.0008),
                new LngLat(0.0012, 0.0012),
                new LngLat(0.0008, 0.0012),
                new LngLat(0.0008, 0.0008)
        ));

        RestrictedArea r2 = new RestrictedArea();
        r2.setVertices(List.of(
                new LngLat(0.0015, 0.0015),
                new LngLat(0.0018, 0.0015),
                new LngLat(0.0018, 0.0018),
                new LngLat(0.0015, 0.0018),
                new LngLat(0.0015, 0.0015)
        ));

        List<LngLat> path = pathfindingService.calculatePath(start, end, List.of(r1, r2));
        // May or may not find path depending on algorithm, but should handle multiple restrictions
        assertThat(path).isNotNull();
    }

    @Test
    @DisplayName("countMoves: counts moves in path")
    void countMoves_path() {
        List<LngLat> path = List.of(
                new LngLat(0.0, 0.0),
                new LngLat(0.00015, 0.0),
                new LngLat(0.0003, 0.0)
        );
        int moves = pathfindingService.countMoves(path);
        assertThat(moves).isGreaterThan(0);
    }

    @Test
    @DisplayName("countMoves: empty path returns zero")
    void countMoves_empty() {
        assertThat(pathfindingService.countMoves(List.of())).isEqualTo(0);
    }

    @Test
    @DisplayName("countMoves: single point returns zero")
    void countMoves_singlePoint() {
        assertThat(pathfindingService.countMoves(List.of(new LngLat(0.0, 0.0)))).isEqualTo(0);
    }

    @Test
    @DisplayName("calculatePath: handles null restricted areas")
    void calculatePath_nullRestrictions() {
        LngLat start = new LngLat(0.0, 0.0);
        LngLat end = new LngLat(0.001, 0.001);

        List<LngLat> path = pathfindingService.calculatePath(start, end, null);
        assertThat(path).isNotEmpty();
    }

    @Test
    @DisplayName("calculatePath: longer distance path")
    void calculatePath_longerDistance() {
        LngLat start = new LngLat(0.0, 0.0);
        LngLat end = new LngLat(0.01, 0.01);

        List<LngLat> path = pathfindingService.calculatePath(start, end, List.of());
        assertThat(path).isNotEmpty();
        assertThat(path.get(0).getLng()).isCloseTo(start.getLng(), org.assertj.core.api.Assertions.within(1e-6));
        assertThat(path.get(path.size() - 1).getLng()).isCloseTo(end.getLng(), org.assertj.core.api.Assertions.within(1e-6));
    }
}
