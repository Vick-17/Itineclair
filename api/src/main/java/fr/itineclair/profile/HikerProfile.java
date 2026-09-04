package fr.itineclair.profile;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "hiker_profiles")
class HikerProfile {

    @Id
    @Column(name = "account_id")
    private UUID accountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "experience_level", nullable = false, length = 24)
    private ExperienceLevel experienceLevel;

    @Column(name = "usual_duration_minutes")
    private Integer usualDurationMinutes;

    @Column(name = "usual_distance_meters")
    private Integer usualDistanceMeters;

    @Column(name = "usual_elevation_gain_meters")
    private Integer usualElevationGainMeters;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected HikerProfile() {
        // Constructeur requis par JPA.
    }

    private HikerProfile(
            UUID accountId,
            HikerProfileCommand command,
            Instant now) {
        this.accountId = accountId;
        this.createdAt = now;
        update(command, now);
    }

    static HikerProfile create(
            UUID accountId,
            HikerProfileCommand command,
            Instant now) {
        return new HikerProfile(accountId, command, now);
    }

    void update(HikerProfileCommand command, Instant now) {
        experienceLevel = command.experienceLevel();
        usualDurationMinutes = command.usualDurationMinutes();
        usualDistanceMeters = command.usualDistanceMeters();
        usualElevationGainMeters = command.usualElevationGainMeters();
        updatedAt = now;
    }

    UUID accountId() {
        return accountId;
    }

    ExperienceLevel experienceLevel() {
        return experienceLevel;
    }

    Integer usualDurationMinutes() {
        return usualDurationMinutes;
    }

    Integer usualDistanceMeters() {
        return usualDistanceMeters;
    }

    Integer usualElevationGainMeters() {
        return usualElevationGainMeters;
    }

    Instant createdAt() {
        return createdAt;
    }

    Instant updatedAt() {
        return updatedAt;
    }
}
