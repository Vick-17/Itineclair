package fr.itineclair.profile.api;

import fr.itineclair.profile.ExperienceLevel;
import fr.itineclair.profile.HikerProfileCommand;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SaveHikerProfileRequest(
        @NotNull(message =
                "Le niveau de pratique auto-déclaré est obligatoire.")
        ExperienceLevel experienceLevel,

        @Min(value = 15, message =
                "La durée habituelle doit être d’au moins 15 minutes.")
        @Max(value = 1440, message =
                "La durée habituelle ne peut pas dépasser 24 heures.")
        Integer usualDurationMinutes,

        @Min(value = 500, message =
                "La distance habituelle doit être d’au moins 500 mètres.")
        @Max(value = 100000, message =
                "La distance habituelle ne peut pas dépasser 100 kilomètres.")
        Integer usualDistanceMeters,

        @Min(value = 0, message =
                "Le dénivelé habituel ne peut pas être négatif.")
        @Max(value = 10000, message =
                "Le dénivelé habituel ne peut pas dépasser 10 000 mètres.")
        Integer usualElevationGainMeters) {

    HikerProfileCommand toCommand() {
        return new HikerProfileCommand(
                experienceLevel,
                usualDurationMinutes,
                usualDistanceMeters,
                usualElevationGainMeters);
    }
}
