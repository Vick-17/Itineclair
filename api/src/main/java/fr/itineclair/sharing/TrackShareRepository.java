package fr.itineclair.sharing;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface TrackShareRepository extends JpaRepository<TrackShare, UUID> {

    Optional<TrackShare> findByTrackIdAndOwnerId(
            UUID trackId,
            UUID ownerId);

    Optional<TrackShare> findByTokenHash(String tokenHash);

    long deleteByTrackIdAndOwnerId(
            UUID trackId,
            UUID ownerId);

    long deleteByExpiresAtLessThanEqual(Instant instant);
}
