package fr.itineclair.track;

public record TrackListEntry(
        TrackSummary track,
        TrackListStatus preparationStatus) {
}
