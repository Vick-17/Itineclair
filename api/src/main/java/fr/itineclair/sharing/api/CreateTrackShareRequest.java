package fr.itineclair.sharing.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateTrackShareRequest(

        @NotNull(message = "La durée du partage est obligatoire.")
        @Min(
                value = 1,
                message = "La durée du partage doit être d’au moins 1 jour.")
        @Max(
                value = 30,
                message = "La durée du partage ne peut pas dépasser 30 jours.")
        Integer durationDays) {
}
