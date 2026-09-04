package fr.itineclair.privacy;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AccountExportSnapshot(
        Account account,
        HikerProfile hikerProfile,
        List<Track> tracks) {

    public AccountExportSnapshot {
        tracks = List.copyOf(tracks);
    }

    public record Account(
            UUID id,
            String email,
            Instant createdAt) {
    }

    public record HikerProfile(
            String experienceLevel,
            Integer usualDurationMinutes,
            Integer usualDistanceMeters,
            Integer usualElevationGainMeters,
            Instant createdAt,
            Instant updatedAt) {
    }

    public record Track(
            UUID id,
            String name,
            String sourceFilename,
            int segmentCount,
            int pointCount,
            int elevationPointCount,
            Instant createdAt,
            TrackFacts facts,
            OutdoorContext outdoorContext,
            Feedback feedback,
            PrivateShare privateShare,
            String gpxFile) {
    }

    public record TrackFacts(
            int version,
            double totalDistanceMeters,
            Double elevationGainMeters,
            Double elevationLossMeters,
            Double minimumElevationMeters,
            Double maximumElevationMeters,
            Double maximumUphillGradePercent,
            Double maximumDownhillGradePercent) {
    }

    public record OutdoorContext(
            Instant plannedStartAt,
            int plannedDurationMinutes,
            String timeZone,
            Instant weatherConsentAt,
            String weatherStatus,
            String weatherSource,
            String weatherAttributionUrl,
            Instant weatherCheckedAt,
            Instant weatherValidFrom,
            Instant weatherValidUntil,
            Double weatherMinimumTemperatureCelsius,
            Double weatherMaximumTemperatureCelsius,
            Double weatherMinimumApparentCelsius,
            Double weatherMaximumApparentCelsius,
            Integer weatherMaximumPrecipitationPercent,
            Double weatherPrecipitationSumMillimeters,
            Double weatherSnowfallSumCentimeters,
            Double weatherMaximumWindSpeedKilometersPerHour,
            Double weatherMaximumWindGustKilometersPerHour,
            Double weatherModelElevationMeters,
            Instant updatedAt) {
    }

    public record Feedback(
            String outcome,
            Integer actualDurationMinutes,
            Integer perceivedEffort,
            String conditionsComparison,
            List<String> observedIssues,
            Instant createdAt,
            Instant updatedAt) {

        public Feedback {
            observedIssues = List.copyOf(observedIssues);
        }
    }

    public record PrivateShare(
            Instant createdAt,
            Instant expiresAt) {
    }

    public record TrackPoint(
            int segmentNumber,
            int pointNumber,
            double latitude,
            double longitude,
            Double elevation,
            Instant recordedAt) {
    }
}
