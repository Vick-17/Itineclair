package fr.itineclair.sharing.api;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import fr.itineclair.analysis.AnalysisCategory;
import fr.itineclair.analysis.AnalysisChecklistItem;
import fr.itineclair.analysis.AnalysisEvidence;
import fr.itineclair.analysis.AnalysisFinding;
import fr.itineclair.analysis.AnalysisSeverity;
import fr.itineclair.analysis.ChecklistStatus;
import fr.itineclair.analysis.EvidenceComparison;
import fr.itineclair.analysis.RuleReviewStatus;
import fr.itineclair.analysis.TrackAnalysis;
import fr.itineclair.outdoor.DaylightCondition;
import fr.itineclair.outdoor.DaylightWindow;
import fr.itineclair.outdoor.OutdoorContextView;
import fr.itineclair.outdoor.WeatherStatus;
import fr.itineclair.outdoor.WeatherSummary;
import fr.itineclair.sharing.SharedTrackReport;
import fr.itineclair.track.TrackFacts;
import fr.itineclair.track.TrackSummary;

public record SharedTrackReportResponse(
        int shareVersion,
        Instant expiresAt,
        SharedTrackResponse track,
        SharedOutdoorContextResponse outdoorContext,
        SharedAnalysisResponse analysis,
        SharedPrivacyResponse privacy) {

    private static final List<String> EXCLUDED_DATA = List.of(
            "Identité et adresse e-mail du propriétaire",
            "Fichier GPX, coordonnées et géométrie du parcours",
            "Nom du fichier, identifiants et date d’import",
            "Retour personnel post-sortie");

    static SharedTrackReportResponse from(SharedTrackReport report) {
        return new SharedTrackReportResponse(
                report.shareVersion(),
                report.expiresAt(),
                SharedTrackResponse.from(report.track()),
                report.outdoorContext() == null
                        ? null
                        : SharedOutdoorContextResponse.from(
                                report.outdoorContext()),
                SharedAnalysisResponse.from(report.analysis()),
                new SharedPrivacyResponse(EXCLUDED_DATA));
    }

    public record SharedTrackResponse(
            int segmentCount,
            int pointCount,
            int elevationPointCount,
            boolean elevationComplete,
            SharedTrackFactsResponse facts) {

        static SharedTrackResponse from(TrackSummary track) {
            return new SharedTrackResponse(
                    track.segmentCount(),
                    track.pointCount(),
                    track.elevationPointCount(),
                    track.elevationPointCount() == track.pointCount(),
                    track.factsAvailable()
                            ? SharedTrackFactsResponse.from(track)
                            : null);
        }
    }

    public record SharedTrackFactsResponse(
            double distanceMeters,
            Double elevationGainMeters,
            Double elevationLossMeters,
            Double minimumElevationMeters,
            Double maximumElevationMeters,
            Double maximumUphillGradePercent,
            Double maximumDownhillGradePercent,
            int gradeMinimumRunMeters) {

        static SharedTrackFactsResponse from(TrackSummary track) {
            return new SharedTrackFactsResponse(
                    track.totalDistanceMeters(),
                    track.elevationGainMeters(),
                    track.elevationLossMeters(),
                    track.minimumElevationMeters(),
                    track.maximumElevationMeters(),
                    track.maximumUphillGradePercent(),
                    track.maximumDownhillGradePercent(),
                    TrackFacts.MINIMUM_GRADE_RUN_METERS);
        }
    }

    public record SharedOutdoorContextResponse(
            boolean planned,
            LocalDateTime plannedStartLocal,
            Instant plannedStartAt,
            Instant plannedEndAt,
            int plannedDurationMinutes,
            String timeZone,
            Instant updatedAt,
            SharedDaylightResponse daylight,
            SharedWeatherResponse weather) {

        static SharedOutdoorContextResponse from(
                OutdoorContextView context) {
            return new SharedOutdoorContextResponse(
                    true,
                    context.plannedStartLocal(),
                    context.plannedStartAt(),
                    context.plannedEndAt(),
                    context.plannedDurationMinutes(),
                    context.timeZone(),
                    context.updatedAt(),
                    SharedDaylightResponse.from(context.daylight()),
                    SharedWeatherResponse.from(context.weather()));
        }
    }

    public record SharedDaylightResponse(
            Instant sunrise,
            Instant sunset,
            Instant civilDawn,
            Instant civilDusk,
            long expectedDaylightMinutes,
            long expectedDarknessMinutes,
            DaylightCondition condition) {

        static SharedDaylightResponse from(DaylightWindow daylight) {
            return new SharedDaylightResponse(
                    daylight.sunrise(),
                    daylight.sunset(),
                    daylight.civilDawn(),
                    daylight.civilDusk(),
                    daylight.expectedDaylightMinutes(),
                    daylight.expectedDarknessMinutes(),
                    daylight.condition());
        }
    }

    public record SharedWeatherResponse(
            WeatherStatus status,
            String source,
            String attributionUrl,
            Instant checkedAt,
            Instant validFrom,
            Instant validUntil,
            Double minimumTemperatureCelsius,
            Double maximumTemperatureCelsius,
            Double minimumApparentCelsius,
            Double maximumApparentCelsius,
            Integer maximumPrecipitationProbabilityPercent,
            Double precipitationSumMillimeters,
            Double snowfallSumCentimeters,
            Double maximumWindSpeedKilometersPerHour,
            Double maximumWindGustKilometersPerHour,
            Double modelElevationMeters) {

        static SharedWeatherResponse from(WeatherSummary weather) {
            return new SharedWeatherResponse(
                    weather.status(),
                    weather.source(),
                    weather.attributionUrl(),
                    weather.checkedAt(),
                    weather.validFrom(),
                    weather.validUntil(),
                    weather.minimumTemperatureCelsius(),
                    weather.maximumTemperatureCelsius(),
                    weather.minimumApparentCelsius(),
                    weather.maximumApparentCelsius(),
                    weather.maximumPrecipitationProbabilityPercent(),
                    weather.precipitationSumMillimeters(),
                    weather.snowfallSumCentimeters(),
                    weather.maximumWindSpeedKilometersPerHour(),
                    weather.maximumWindGustKilometersPerHour(),
                    weather.modelElevationMeters());
        }
    }

    public record SharedAnalysisResponse(
            int ruleSetVersion,
            RuleReviewStatus reviewStatus,
            Instant generatedAt,
            SharedAnalysisSourceResponse sourceSnapshot,
            List<SharedFindingResponse> findings,
            List<SharedChecklistItemResponse> checklist,
            List<String> limitations) {

        static SharedAnalysisResponse from(TrackAnalysis analysis) {
            return new SharedAnalysisResponse(
                    analysis.ruleSetVersion(),
                    analysis.reviewStatus(),
                    analysis.generatedAt(),
                    new SharedAnalysisSourceResponse(
                            analysis.sourceSnapshot().factsVersion(),
                            analysis.sourceSnapshot()
                                    .outdoorContextUpdatedAt(),
                            analysis.sourceSnapshot().weatherCheckedAt()),
                    analysis.findings()
                            .stream()
                            .map(SharedFindingResponse::from)
                            .toList(),
                    analysis.checklist()
                            .stream()
                            .map(SharedChecklistItemResponse::from)
                            .toList(),
                    analysis.limitations());
        }
    }

    public record SharedAnalysisSourceResponse(
            Integer factsVersion,
            Instant outdoorContextUpdatedAt,
            Instant weatherCheckedAt) {
    }

    public record SharedFindingResponse(
            String code,
            AnalysisCategory category,
            AnalysisSeverity severity,
            String title,
            String explanation,
            String action,
            List<SharedEvidenceResponse> evidence) {

        static SharedFindingResponse from(AnalysisFinding finding) {
            return new SharedFindingResponse(
                    finding.code(),
                    finding.category(),
                    finding.severity(),
                    finding.title(),
                    finding.explanation(),
                    finding.action(),
                    finding.evidence()
                            .stream()
                            .map(SharedEvidenceResponse::from)
                            .toList());
        }
    }

    public record SharedEvidenceResponse(
            String metric,
            String label,
            double observedValue,
            String unit,
            EvidenceComparison comparison,
            double thresholdValue) {

        static SharedEvidenceResponse from(AnalysisEvidence evidence) {
            return new SharedEvidenceResponse(
                    evidence.metric(),
                    evidence.label(),
                    evidence.observedValue(),
                    evidence.unit(),
                    evidence.comparison(),
                    evidence.thresholdValue());
        }
    }

    public record SharedChecklistItemResponse(
            String code,
            ChecklistStatus status,
            String title,
            String detail) {

        static SharedChecklistItemResponse from(
                AnalysisChecklistItem item) {
            return new SharedChecklistItemResponse(
                    item.code(),
                    item.status(),
                    item.title(),
                    item.detail());
        }
    }

    public record SharedPrivacyResponse(
            List<String> excludedData) {

        public SharedPrivacyResponse {
            excludedData = List.copyOf(excludedData);
        }
    }
}
