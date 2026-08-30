package fr.itineclair.track;

public record TrackFacts(
        int version,
        double totalDistanceMeters,
        Double elevationGainMeters,
        Double elevationLossMeters,
        Double minimumElevationMeters,
        Double maximumElevationMeters,
        Double maximumUphillGradePercent,
        Double maximumDownhillGradePercent) {

    public static final int CURRENT_VERSION = 1;

    public static final int MINIMUM_GRADE_RUN_METERS = 25;

    public TrackFacts {
        if (version != CURRENT_VERSION) {
            throw new IllegalArgumentException(
                    "Unsupported track facts version.");
        }

        requireFiniteNonNegative(
                totalDistanceMeters,
                "totalDistanceMeters");

        requireOptionalFiniteNonNegative(
                elevationGainMeters,
                "elevationGainMeters");
        requireOptionalFiniteNonNegative(
                elevationLossMeters,
                "elevationLossMeters");
        requireOptionalFinite(
                minimumElevationMeters,
                "minimumElevationMeters");
        requireOptionalFinite(
                maximumElevationMeters,
                "maximumElevationMeters");
        requireOptionalFiniteNonNegative(
                maximumUphillGradePercent,
                "maximumUphillGradePercent");
        requireOptionalFiniteNonNegative(
                maximumDownhillGradePercent,
                "maximumDownhillGradePercent");

        boolean hasElevationTotals =
                elevationGainMeters != null
                        && elevationLossMeters != null;

        boolean hasElevationRange =
                minimumElevationMeters != null
                        && maximumElevationMeters != null;

        if ((elevationGainMeters == null)
                != (elevationLossMeters == null)
                || (minimumElevationMeters == null)
                != (maximumElevationMeters == null)
                || hasElevationTotals != hasElevationRange) {
            throw new IllegalArgumentException(
                    "Elevation facts must be consistently present.");
        }

        if (hasElevationRange
                && minimumElevationMeters
                > maximumElevationMeters) {
            throw new IllegalArgumentException(
                    "Minimum elevation exceeds maximum elevation.");
        }
    }

    private static void requireOptionalFinite(
            Double value,
            String field) {
        if (value != null && !Double.isFinite(value)) {
            throw new IllegalArgumentException(
                    field + " must be finite.");
        }
    }

    private static void requireOptionalFiniteNonNegative(
            Double value,
            String field) {
        if (value != null) {
            requireFiniteNonNegative(value, field);
        }
    }

    private static void requireFiniteNonNegative(
            double value,
            String field) {
        if (!Double.isFinite(value) || value < 0.0) {
            throw new IllegalArgumentException(
                    field + " must be finite and non-negative.");
        }
    }
}
