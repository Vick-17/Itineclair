package fr.itineclair.sharing;

import java.time.Instant;

import fr.itineclair.analysis.TrackAnalysis;
import fr.itineclair.outdoor.OutdoorContextView;
import fr.itineclair.track.TrackSummary;

public record SharedTrackReport(
        int shareVersion,
        Instant expiresAt,
        TrackSummary track,
        OutdoorContextView outdoorContext,
        TrackAnalysis analysis) {

    public static final int CURRENT_SHARE_VERSION = 1;
}
