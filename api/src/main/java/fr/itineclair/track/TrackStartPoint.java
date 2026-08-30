package fr.itineclair.track;

/**
 * Coordonnée conservée côté serveur pour les calculs locaux et les appels
 * explicitement consentis. Elle ne fait partie d'aucun contrat HTTP.
 */
public record TrackStartPoint(
        double latitude,
        double longitude,
        Double elevationMeters) {
}
