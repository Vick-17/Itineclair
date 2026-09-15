package fr.itineclair.track;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TrackListService {

    private final TrackImportService trackImportService;
    private final TrackListProgressStore progressStore;
    private final Clock clock;

    public TrackListService(
            TrackImportService trackImportService,
            TrackListProgressStore progressStore,
            Clock clock) {
        this.trackImportService = trackImportService;
        this.progressStore = progressStore;
        this.clock = clock;
    }

    @Transactional
    public List<TrackListEntry> listTracks(UUID ownerId) {
        Objects.requireNonNull(ownerId, "ownerId");

        List<TrackSummary> tracks = trackImportService.listTracks(ownerId);

        if (tracks.isEmpty()) {
            return List.of();
        }

        Map<UUID, TrackListProgress> progressByTrack =
                progressStore.findAllByOwnerId(ownerId);
        Instant now = clock.instant();

        return tracks.stream()
                .map(track -> toEntry(
                        track,
                        progressByTrack.getOrDefault(
                                track.id(),
                                TrackListProgress.empty()),
                        now))
                .toList();
    }

    private TrackListEntry toEntry(
            TrackSummary track,
            TrackListProgress progress,
            Instant now) {
        return new TrackListEntry(
                track,
                TrackListStatus.resolve(track, progress, now));
    }
}
