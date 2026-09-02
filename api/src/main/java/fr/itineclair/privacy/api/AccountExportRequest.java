package fr.itineclair.privacy.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AccountExportRequest(

        @NotBlank(message = "Le mot de passe courant est obligatoire.")
        @Size(
                max = 128,
                message = "Le mot de passe courant ne peut pas dépasser 128 caractères.")
        String currentPassword) {
}
