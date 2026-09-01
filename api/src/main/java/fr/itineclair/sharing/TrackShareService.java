package fr.itineclair.sharing;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fr.itineclair.analysis.TrackAnalysis;
import fr.itineclair.analysis.TrackAnalysisService;
import fr.itineclair.outdoor.OutdoorContextService;
import fr.itineclair.outdoor.OutdoorContextView;
import fr.itineclair.track.TrackImportService;
import fr.itineclair.track.TrackSummary;

@Service
public class TrackShareService {

    private static final int MINIMUM_DURATION_DAYS = 1;
    private static final int MAXIMUM_DURATION_DAYS = 30;

    private final TrackShareRepository shareRepository;
    private final ShareTokenCodec tokenCodec;
    private final TrackImportService trackImportService;
    private final OutdoorContextService outdoorContextService;
    private final TrackAnalysisService trackAnalysisService;
    private final Clock clock;

    public TrackShareService(
            TrackShareRepository shareRepository,
            ShareTokenCodec tokenCodec,
            TrackImportService trackImportService,
            OutdoorContextService outdoorContextService,
            TrackAnalysisService trackAnalysisService,
            Clock clock) {
        this.shareRepository = shareRepository;
        this.tokenCodec = tokenCodec;
        this.trackImportService = trackImportService;
        this.outdoorContextService = outdoorContextService;
        this.trackAnalysisService = trackAnalysisService;
        this.clock = clock;
    }

    @Transactional
    public TrackShareStatus getStatus(
            UUID ownerId,
            UUID trackId) {
        requireOwnedTrack(ownerId, trackId);
        Instant now = clock.instant();

        return shareRepository
                .findByTrackIdAndOwnerId(trackId, ownerId)
                .filter(share -> share.isActiveAt(now))
                .map(TrackShareStatus::active)
                .orElseGet(TrackShareStatus::inactive);
    }

    @Transactional
    public SharedTrackReport preview(
            UUID ownerId,
            UUID trackId) {
        return buildReport(ownerId, trackId, null);
    }

    @Transactional
    public CreatedTrackShare createOrRotate(
            UUID ownerId,
            UUID trackId,
            int durationDays) {
        requireOwnedTrack(ownerId, trackId);
        validateDuration(durationDays);

        Instant createdAt = clock.instant();
        Instant expiresAt = createdAt.plus(
                durationDays,
                ChronoUnit.DAYS);
        ShareTokenCodec.TokenMaterial token = tokenCodec.generate();

        TrackShare share = shareRepository
                .findByTrackIdAndOwnerId(trackId, ownerId)
                .orElseGet(() -> TrackShare.create(
                        trackId,
                        ownerId,
                        token.hash(),
                        expiresAt,
                        createdAt));

        share.activate(token.hash(), expiresAt, createdAt);
        shareRepository.saveAndFlush(share);

        return new CreatedTrackShare(
                token.token(),
                expiresAt,
                createdAt);
    }

    @Transactional
    public void revoke(
            UUID ownerId,
            UUID trackId) {
        requireOwnedTrack(ownerId, trackId);
        shareRepository.deleteByTrackIdAndOwnerId(trackId, ownerId);
    }

    @Transactional
    public SharedTrackReport getPublicReport(String presentedToken) {
        String tokenHash = tokenCodec
                .hashPresented(presentedToken)
                .orElseThrow(SharedReportNotFoundException::new);
        Instant now = clock.instant();

        TrackShare share = shareRepository
                .findByTokenHash(tokenHash)
                .filter(candidate -> candidate.isActiveAt(now))
                .orElseThrow(SharedReportNotFoundException::new);

        return buildReport(
                share.ownerId(),
                share.trackId(),
                share.expiresAt());
    }

    private SharedTrackReport buildReport(
            UUID ownerId,
            UUID trackId,
            Instant expiresAt) {
        TrackSummary track = trackImportService.getTrack(ownerId, trackId);
        OutdoorContextView outdoorContext = outdoorContextService
                .getContext(ownerId, trackId)
                .orElse(null);
        TrackAnalysis analysis = trackAnalysisService.analyze(
                ownerId,
                trackId);

        return new SharedTrackReport(
                SharedTrackReport.CURRENT_SHARE_VERSION,
                expiresAt,
                track,
                outdoorContext,
                analysis);
    }

    private void requireOwnedTrack(
            UUID ownerId,
            UUID trackId) {
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(trackId, "trackId");
        trackImportService.getTrack(ownerId, trackId);
    }

    private void validateDuration(int durationDays) {
        if (durationDays < MINIMUM_DURATION_DAYS
                || durationDays > MAXIMUM_DURATION_DAYS) {
            throw new InvalidTrackShareException(
                    "La durée du partage doit être comprise entre 1 et 30 jours.");
        }
    }
}
