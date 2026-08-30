package fr.itineclair.analysis;

import java.time.Clock;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import fr.itineclair.outdoor.OutdoorContextService;
import fr.itineclair.outdoor.OutdoorContextView;
import fr.itineclair.track.TrackImportService;
import fr.itineclair.track.TrackSummary;

@Service
public class TrackAnalysisService {

    private final TrackImportService trackImportService;
    private final OutdoorContextService outdoorContextService;
    private final TrackRuleAnalysisEngine analysisEngine;
    private final Clock clock;

    public TrackAnalysisService(
            TrackImportService trackImportService,
            OutdoorContextService outdoorContextService,
            TrackRuleAnalysisEngine analysisEngine,
            Clock clock) {
        this.trackImportService = trackImportService;
        this.outdoorContextService = outdoorContextService;
        this.analysisEngine = analysisEngine;
        this.clock = clock;
    }

    public TrackAnalysis analyze(
            UUID ownerId,
            UUID trackId) {
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(trackId, "trackId");

        TrackSummary track = trackImportService.getTrack(
                ownerId,
                trackId);
        Optional<OutdoorContextView> context = outdoorContextService
                .getContext(ownerId, trackId);

        return analysisEngine.analyze(
                track,
                context,
                clock.instant());
    }
}
