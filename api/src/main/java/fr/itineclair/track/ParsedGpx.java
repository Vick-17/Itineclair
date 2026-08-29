package fr.itineclair.track;

import java.util.List;

record ParsedGpx(
        String name,
        int segmentCount,
        List<ParsedTrackPoint> points) {

    ParsedGpx {
        points = List.copyOf(points);
    }

    int pointCount() {
        return points.size();
    }

    int elevationPointCount() {
        return (int) points.stream()
                .filter(point -> point.elevation() != null)
                .count();
    }
}
