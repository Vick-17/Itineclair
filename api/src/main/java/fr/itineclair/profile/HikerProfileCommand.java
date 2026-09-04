package fr.itineclair.profile;

public record HikerProfileCommand(
        ExperienceLevel experienceLevel,
        Integer usualDurationMinutes,
        Integer usualDistanceMeters,
        Integer usualElevationGainMeters) {
}
