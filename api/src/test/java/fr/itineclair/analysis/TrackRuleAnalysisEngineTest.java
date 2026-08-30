package fr.itineclair.analysis;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import fr.itineclair.outdoor.DaylightCondition;
import fr.itineclair.outdoor.DaylightWindow;
import fr.itineclair.outdoor.OutdoorContextView;
import fr.itineclair.outdoor.WeatherStatus;
import fr.itineclair.outdoor.WeatherSummary;
import fr.itineclair.track.TrackSummary;

import static org.assertj.core.api.Assertions.assertThat;

class TrackRuleAnalysisEngineTest {

    private static final Instant NOW =
            Instant.parse("2026-08-30T12:00:00Z");

    private final TrackRuleAnalysisEngine engine =
            new TrackRuleAnalysisEngine();

    @Test
    void degradesExplicitlyWhenNoPlanExists() {
        TrackAnalysis analysis = engine.analyze(
                track(12_000.0, 500.0, 1_800.0, 20.0, true),
                Optional.empty(),
                NOW);

        assertThat(analysis.ruleSetVersion()).isEqualTo(1);
        assertThat(analysis.reviewStatus()).isEqualTo(
                RuleReviewStatus.PROTOTYPE_AWAITING_EXPERT_REVIEW);
        assertThat(analysis.findings())
                .extracting(AnalysisFinding::code)
                .containsExactly("PLAN_NOT_CONFIGURED");
        assertThat(analysis.checklist())
                .filteredOn(item -> item.code()
                        .equals("PLANNED_WINDOW_AND_LIGHT"))
                .extracting(AnalysisChecklistItem::status)
                .containsExactly(ChecklistStatus.TO_VERIFY);
        assertThat(analysis.limitations())
                .anyMatch(value -> value.contains("alerte officielle"));
    }

    @ParameterizedTest
    @CsvSource({
            "19999, NONE",
            "20000, CAUTION",
            "34999, CAUTION",
            "35000, STRONG_CAUTION"
    })
    void appliesDistanceThresholdsAtDocumentedBoundaries(
            double distanceMeters,
            String expectedSeverity) {
        TrackAnalysis analysis = engine.analyze(
                track(distanceMeters, 500.0, 1_800.0, 20.0, true),
                Optional.empty(),
                NOW);

        Optional<AnalysisFinding> finding = analysis.findings()
                .stream()
                .filter(item -> item.code().equals("DISTANCE_LOAD"))
                .findFirst();

        if (expectedSeverity.equals("NONE")) {
            assertThat(finding).isEmpty();
            return;
        }

        assertThat(finding).isPresent();
        assertThat(finding.orElseThrow().severity().name())
                .isEqualTo(expectedSeverity);
        assertThat(finding.orElseThrow().evidence())
                .singleElement()
                .satisfies(evidence -> {
                    assertThat(evidence.metric())
                            .isEqualTo("distanceKilometers");
                    assertThat(evidence.observedValue())
                            .isEqualTo(distanceMeters / 1_000.0);
                });
    }

    @Test
    void keepsDimensionsSeparateAndExplainsStrongSignals() {
        TrackSummary track = track(
                40_000.0,
                1_800.0,
                3_200.0,
                55.0,
                true);
        OutdoorContextView context = context(
                720,
                150,
                availableWeather(
                        NOW.minusSeconds(13 * 3600),
                        -2.0,
                        36.0,
                        90,
                        20.0,
                        6.0,
                        75.0,
                        1_000.0));

        TrackAnalysis analysis = engine.analyze(
                track,
                Optional.of(context),
                NOW);

        assertThat(analysis.findings())
                .filteredOn(finding -> finding.severity()
                        == AnalysisSeverity.STRONG_CAUTION)
                .extracting(AnalysisFinding::code)
                .contains(
                        "DISTANCE_LOAD",
                        "ELEVATION_GAIN_LOAD",
                        "HIGH_ALTITUDE",
                        "STEEP_GRADE",
                        "LONG_PLANNED_DURATION",
                        "EXPECTED_DARKNESS",
                        "WEATHER_FORECAST_STALE",
                        "WEATHER_ELEVATION_GAP",
                        "STRONG_GUSTS_AT_START",
                        "PRECIPITATION_AT_START",
                        "SNOWFALL_AT_START",
                        "LOW_APPARENT_TEMPERATURE",
                        "HIGH_TEMPERATURE_AT_START");
        assertThat(analysis.findings())
                .extracting(AnalysisFinding::category)
                .contains(
                        AnalysisCategory.PHYSICAL_LOAD,
                        AnalysisCategory.ROUTE_CHARACTERISTICS,
                        AnalysisCategory.LIGHT,
                        AnalysisCategory.WEATHER,
                        AnalysisCategory.DATA_QUALITY);
        assertThat(analysis.findings())
                .allSatisfy(finding -> {
                    assertThat(finding.explanation()).isNotBlank();
                    assertThat(finding.action()).isNotBlank();
                });
    }

    @Test
    void marksWeatherAsUnknownInsteadOfInventingSignals() {
        WeatherSummary unavailable = new WeatherSummary(
                WeatherStatus.UNAVAILABLE,
                "Open-Meteo",
                "https://open-meteo.com/",
                NOW,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);

        TrackAnalysis analysis = engine.analyze(
                track(12_000.0, 500.0, 1_800.0, 20.0, true),
                Optional.of(context(360, 0, unavailable)),
                NOW);

        assertThat(analysis.findings())
                .extracting(AnalysisFinding::code)
                .containsExactly("WEATHER_UNAVAILABLE");
        assertThat(analysis.checklist())
                .filteredOn(item -> item.code().equals("POINT_WEATHER"))
                .extracting(AnalysisChecklistItem::status)
                .containsExactly(ChecklistStatus.TO_VERIFY);
    }

    @Test
    void exposesPartialElevationAsDataQualityNotAsCertainty() {
        TrackAnalysis analysis = engine.analyze(
                track(12_000.0, 500.0, 1_800.0, 20.0, false),
                Optional.empty(),
                NOW);

        AnalysisFinding finding = analysis.findings()
                .stream()
                .filter(item -> item.code()
                        .equals("ELEVATION_INCOMPLETE"))
                .findFirst()
                .orElseThrow();

        assertThat(finding.category())
                .isEqualTo(AnalysisCategory.DATA_QUALITY);
        assertThat(finding.evidence())
                .singleElement()
                .satisfies(evidence -> {
                    assertThat(evidence.observedValue()).isEqualTo(80.0);
                    assertThat(evidence.thresholdValue()).isEqualTo(100.0);
                    assertThat(evidence.comparison())
                            .isEqualTo(EvidenceComparison.LESS_THAN);
                });
    }

    private TrackSummary track(
            double distanceMeters,
            Double elevationGainMeters,
            Double maximumElevationMeters,
            Double maximumGradePercent,
            boolean completeElevation) {
        int pointCount = 100;
        int elevationPointCount = completeElevation ? 100 : 80;
        return new TrackSummary(
                UUID.fromString("ce7b58c7-d6f4-4a26-8bad-68265bdb7bbf"),
                "Boucle test",
                "boucle.gpx",
                1,
                pointCount,
                elevationPointCount,
                1,
                distanceMeters,
                elevationGainMeters,
                elevationGainMeters,
                1_000.0,
                maximumElevationMeters,
                maximumGradePercent,
                maximumGradePercent,
                Instant.parse("2026-08-29T12:00:00Z"));
    }

    private OutdoorContextView context(
            int durationMinutes,
            long darknessMinutes,
            WeatherSummary weather) {
        Instant start = Instant.parse("2026-08-31T06:00:00Z");
        return new OutdoorContextView(
                LocalDateTime.parse("2026-08-31T08:00:00"),
                start,
                start.plusSeconds(durationMinutes * 60L),
                durationMinutes,
                "Europe/Paris",
                NOW.minusSeconds(300),
                new DaylightWindow(
                        Instant.parse("2026-08-31T04:51:00Z"),
                        Instant.parse("2026-08-31T18:16:00Z"),
                        Instant.parse("2026-08-31T04:19:00Z"),
                        Instant.parse("2026-08-31T18:48:00Z"),
                        durationMinutes - darknessMinutes,
                        darknessMinutes,
                        DaylightCondition.NORMAL),
                weather);
    }

    private WeatherSummary availableWeather(
            Instant checkedAt,
            double minimumApparentCelsius,
            double maximumTemperatureCelsius,
            int precipitationProbabilityPercent,
            double precipitationMillimeters,
            double snowfallCentimeters,
            double gustKilometersPerHour,
            double modelElevationMeters) {
        return new WeatherSummary(
                WeatherStatus.AVAILABLE,
                "Open-Meteo",
                "https://open-meteo.com/",
                checkedAt,
                Instant.parse("2026-08-31T06:00:00Z"),
                Instant.parse("2026-08-31T18:00:00Z"),
                4.0,
                maximumTemperatureCelsius,
                minimumApparentCelsius,
                maximumTemperatureCelsius - 1.0,
                precipitationProbabilityPercent,
                precipitationMillimeters,
                snowfallCentimeters,
                40.0,
                gustKilometersPerHour,
                modelElevationMeters);
    }
}
