package fr.itineclair.track;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class TrackFactsCalculatorTest {

    private final TrackFactsCalculator calculator =
            new TrackFactsCalculator();

    @Test
    void calculatesWgs84DistanceAndElevationFacts() {
        TrackFacts facts = calculator.calculate(List.of(
                point(0, 0, 45.0, 6.0, 1_000.0),
                point(0, 1, 45.0, 6.001, 1_010.0),
                point(0, 2, 45.001, 6.001, 1_005.0),
                point(1, 0, 46.0, 7.0, 2_000.0)));

        assertThat(facts.version())
                .isEqualTo(TrackFacts.CURRENT_VERSION);
        assertThat(facts.totalDistanceMeters())
                .isBetween(189.0, 191.0);
        assertThat(facts.elevationGainMeters())
                .isEqualTo(10.0);
        assertThat(facts.elevationLossMeters())
                .isEqualTo(5.0);
        assertThat(facts.minimumElevationMeters())
                .isEqualTo(1_000.0);
        assertThat(facts.maximumElevationMeters())
                .isEqualTo(2_000.0);
        assertThat(facts.maximumUphillGradePercent())
                .isBetween(12.0, 13.0);
        assertThat(facts.maximumDownhillGradePercent())
                .isBetween(4.0, 5.0);
    }

    @Test
    void usesTheWgs84EllipsoidForGeodesicDistance() {
        TrackFacts facts = calculator.calculate(List.of(
                point(0, 0, 0.0, 0.0, null),
                point(0, 1, 0.0, 1.0, null)));

        assertThat(facts.totalDistanceMeters())
                .isCloseTo(
                        111_319.490_793,
                        within(0.001));
        assertThat(facts.elevationGainMeters())
                .isNull();
        assertThat(facts.minimumElevationMeters())
                .isNull();
    }

    @Test
    void neverBridgesDistinctGpxSegments() {
        TrackFacts facts = calculator.calculate(List.of(
                point(0, 0, 45.0, 6.0, 1_000.0),
                point(1, 0, 46.0, 7.0, 2_000.0)));

        assertThat(facts.totalDistanceMeters())
                .isZero();
        assertThat(facts.elevationGainMeters())
                .isZero();
        assertThat(facts.elevationLossMeters())
                .isZero();
        assertThat(facts.maximumUphillGradePercent())
                .isNull();
    }

    @Test
    void neverInfersElevationAcrossMissingSamples() {
        TrackFacts facts = calculator.calculate(List.of(
                point(0, 0, 45.0, 6.0, 1_000.0),
                point(0, 1, 45.0, 6.001, null),
                point(0, 2, 45.0, 6.002, 1_020.0)));

        assertThat(facts.totalDistanceMeters())
                .isGreaterThan(150.0);
        assertThat(facts.elevationGainMeters())
                .isZero();
        assertThat(facts.elevationLossMeters())
                .isZero();
        assertThat(facts.minimumElevationMeters())
                .isEqualTo(1_000.0);
        assertThat(facts.maximumElevationMeters())
                .isEqualTo(1_020.0);
        assertThat(facts.maximumUphillGradePercent())
                .isNull();
        assertThat(facts.maximumDownhillGradePercent())
                .isNull();
    }

    private ParsedTrackPoint point(
            int segmentNumber,
            int pointNumber,
            double latitude,
            double longitude,
            Double elevation) {
        return new ParsedTrackPoint(
                segmentNumber,
                pointNumber,
                latitude,
                longitude,
                elevation,
                null);
    }
}
