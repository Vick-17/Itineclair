package fr.itineclair.outdoor.api;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SaveOutdoorContextRequest(

        @NotNull(message = "L’heure de départ est obligatoire.")
        LocalDateTime plannedStartLocal,

        @Min(
                value = 30,
                message = "La durée prévue doit être d’au moins 30 minutes.")
        @Max(
                value = 1440,
                message = "La durée prévue ne peut pas dépasser 24 heures.")
        int plannedDurationMinutes,

        @NotBlank(message = "Le fuseau horaire est obligatoire.")
        @Size(
                max = 64,
                message = "Le fuseau horaire ne peut pas dépasser 64 caractères.")
        String timeZone,

        @NotNull(
                message = "Le choix de partage météo doit être explicite.")
        Boolean shareStartPointWithWeatherProvider) {
}
