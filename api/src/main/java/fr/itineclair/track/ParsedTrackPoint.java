package fr.itineclair.track;

import java.time.Instant;

record ParsedTrackPoint(
        int segmentNumber,
        int pointNumber,
        double latitude,
        double longitude,
        Double elevation,
        Instant recordedAt) {
}
