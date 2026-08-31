package fr.itineclair.feedback;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface TrackFeedbackRepository extends JpaRepository<TrackFeedback, UUID> {
}
