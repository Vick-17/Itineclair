package fr.itineclair.profile.api;

import java.time.Instant;

import fr.itineclair.profile.ExperienceLevel;
import fr.itineclair.profile.HikerProfileView;

public record HikerProfileResponse(
        boolean configured,
        ExperienceLevel experienceLevel,
        Integer usualDurationMinutes,
        Integer usualDistanceMeters,
        Integer usualElevationGainMeters,
        Instant createdAt,
        Instant updatedAt) {

    static HikerProfileResponse notConfigured() {
        return new HikerProfileResponse(
                false,
                null,
                null,
                null,
                null,
                null,
                null);
    }

    static HikerProfileResponse from(HikerProfileView profile) {
        return new HikerProfileResponse(
                true,
                profile.experienceLevel(),
                profile.usualDurationMinutes(),
                profile.usualDistanceMeters(),
                profile.usualElevationGainMeters(),
                profile.createdAt(),
                profile.updatedAt());
    }
}
