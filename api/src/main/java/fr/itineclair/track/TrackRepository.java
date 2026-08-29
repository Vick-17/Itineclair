package fr.itineclair.track;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface TrackRepository extends JpaRepository<Track, UUID> {

    List<Track> findAllByOwnerIdOrderByCreatedAtDesc(UUID ownerId);
}
