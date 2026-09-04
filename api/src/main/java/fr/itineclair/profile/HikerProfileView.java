package fr.itineclair.profile;

import java.time.Instant;
import java.util.UUID;

public record HikerProfileView(
        UUID accountId,
        ExperienceLevel experienceLevel,
        Integer usualDurationMinutes,
        Integer usualDistanceMeters,
        Integer usualElevationGainMeters,
        Instant createdAt,
        Instant updatedAt) {

    static HikerProfileView from(HikerProfile profile) {
        return new HikerProfileView(
                profile.accountId(),
                profile.experienceLevel(),
                profile.usualDurationMinutes(),
                profile.usualDistanceMeters(),
                profile.usualElevationGainMeters(),
                profile.createdAt(),
                profile.updatedAt());
    }
}
