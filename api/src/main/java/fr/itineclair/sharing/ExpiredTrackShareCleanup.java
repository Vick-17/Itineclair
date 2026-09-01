package fr.itineclair.sharing;

import java.time.Clock;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class ExpiredTrackShareCleanup {

    private final TrackShareRepository shareRepository;
    private final Clock clock;

    ExpiredTrackShareCleanup(
            TrackShareRepository shareRepository,
            Clock clock) {
        this.shareRepository = shareRepository;
        this.clock = clock;
    }

    @Scheduled(
            cron = "${itineclair.sharing.cleanup-cron:0 17 3 * * *}",
            zone = "UTC")
    @Transactional
    public void deleteExpiredShares() {
        shareRepository.deleteByExpiresAtLessThanEqual(
                clock.instant());
    }
}
