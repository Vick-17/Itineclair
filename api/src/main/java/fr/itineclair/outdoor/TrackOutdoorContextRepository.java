package fr.itineclair.outdoor;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface TrackOutdoorContextRepository
        extends JpaRepository<TrackOutdoorContext, UUID> {
}
