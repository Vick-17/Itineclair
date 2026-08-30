package fr.itineclair.track;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Component;

import net.sf.geographiclib.Geodesic;

@Component
class TrackFactsCalculator {

    private static final Geodesic WGS84 = Geodesic.WGS84;

    TrackFacts calculate(List<ParsedTrackPoint> points) {
        Objects.requireNonNull(points, "points");

        double totalDistanceMeters = 0.0;
        double elevationGainMeters = 0.0;
        double elevationLossMeters = 0.0;

        Double minimumElevationMeters = null;
        Double maximumElevationMeters = null;
        Double maximumUphillGradePercent = null;
        Double maximumDownhillGradePercent = null;

        ParsedTrackPoint previous = null;
        int currentSegment = Integer.MIN_VALUE;
        double segmentDistanceMeters = 0.0;
        Deque<ElevationSample> gradeSamples =
                new ArrayDeque<>();

        for (ParsedTrackPoint point : points) {
            Double elevation = point.elevation();

            if (elevation != null) {
                minimumElevationMeters =
                        minimumElevationMeters == null
                                ? elevation
                                : Math.min(
                                        minimumElevationMeters,
                                        elevation);

                maximumElevationMeters =
                        maximumElevationMeters == null
                                ? elevation
                                : Math.max(
                                        maximumElevationMeters,
                                        elevation);
            }

            boolean startsSegment =
                    previous == null
                            || point.segmentNumber()
                            != currentSegment;

            if (startsSegment) {
                currentSegment = point.segmentNumber();
                segmentDistanceMeters = 0.0;
                gradeSamples.clear();

                if (elevation != null) {
                    gradeSamples.addLast(
                            new ElevationSample(
                                    segmentDistanceMeters,
                                    elevation));
                }

                previous = point;
                continue;
            }

            double horizontalDistanceMeters =
                    horizontalDistance(previous, point);

            totalDistanceMeters += horizontalDistanceMeters;

            if (!Double.isFinite(totalDistanceMeters)) {
                throw invalidCalculation();
            }

            segmentDistanceMeters +=
                    horizontalDistanceMeters;

            if (previous.elevation() != null
                    && elevation != null) {
                double elevationChange =
                        elevation - previous.elevation();

                if (elevationChange > 0.0) {
                    elevationGainMeters += elevationChange;
                } else if (elevationChange < 0.0) {
                    elevationLossMeters -= elevationChange;
                }
            }

            if (elevation == null) {
                gradeSamples.clear();
            } else if (previous.elevation() == null) {
                gradeSamples.clear();
                gradeSamples.addLast(
                        new ElevationSample(
                                segmentDistanceMeters,
                                elevation));
            } else {
                gradeSamples.addLast(
                        new ElevationSample(
                                segmentDistanceMeters,
                                elevation));

                while (gradeSamples.size() > 1) {
                    ElevationSample first =
                            gradeSamples.removeFirst();

                    ElevationSample second =
                            gradeSamples.peekFirst();

                    if (second != null
                            && segmentDistanceMeters
                            - second.distanceMeters()
                            >= TrackFacts
                                    .MINIMUM_GRADE_RUN_METERS) {
                        continue;
                    }

                    gradeSamples.addFirst(first);
                    break;
                }

                ElevationSample start =
                        gradeSamples.peekFirst();

                if (start != null) {
                    double runMeters =
                            segmentDistanceMeters
                                    - start.distanceMeters();

                    if (runMeters
                            >= TrackFacts
                                    .MINIMUM_GRADE_RUN_METERS) {
                        double gradePercent =
                                100.0
                                        * (elevation
                                        - start.elevationMeters())
                                        / runMeters;

                        if (!Double.isFinite(gradePercent)) {
                            throw invalidCalculation();
                        }

                        if (gradePercent > 0.0) {
                            maximumUphillGradePercent =
                                    maximumUphillGradePercent
                                            == null
                                            ? gradePercent
                                            : Math.max(
                                                    maximumUphillGradePercent,
                                                    gradePercent);
                        } else if (gradePercent < 0.0) {
                            double downhillGradePercent =
                                    -gradePercent;

                            maximumDownhillGradePercent =
                                    maximumDownhillGradePercent
                                            == null
                                            ? downhillGradePercent
                                            : Math.max(
                                                    maximumDownhillGradePercent,
                                                    downhillGradePercent);
                        }
                    }
                }
            }

            previous = point;
        }

        boolean hasElevation =
                minimumElevationMeters != null;

        return new TrackFacts(
                TrackFacts.CURRENT_VERSION,
                totalDistanceMeters,
                hasElevation ? elevationGainMeters : null,
                hasElevation ? elevationLossMeters : null,
                minimumElevationMeters,
                maximumElevationMeters,
                maximumUphillGradePercent,
                maximumDownhillGradePercent);
    }

    private double horizontalDistance(
            ParsedTrackPoint first,
            ParsedTrackPoint second) {
        double distanceMeters = WGS84.Inverse(
                first.latitude(),
                first.longitude(),
                second.latitude(),
                second.longitude())
                .s12;

        if (!Double.isFinite(distanceMeters)
                || distanceMeters < 0.0) {
            throw invalidCalculation();
        }

        return distanceMeters;
    }

    private InvalidGpxException invalidCalculation() {
        return new InvalidGpxException(
                "Les faits de la trace n’ont pas pu être calculés.");
    }

    private record ElevationSample(
            double distanceMeters,
            double elevationMeters) {
    }
}
