package fr.itineclair.privacy.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DeleteAccountRequest(

        @NotBlank(message = "Le mot de passe courant est obligatoire.")
        @Size(
                max = 128,
                message = "Le mot de passe courant ne peut pas dépasser 128 caractères.")
        String currentPassword,

        @NotBlank(message = "L’adresse e-mail de confirmation est obligatoire.")
        @Email(message = "L’adresse e-mail de confirmation n’est pas valide.")
        @Size(
                max = 254,
                message = "L’adresse e-mail ne peut pas dépasser 254 caractères.")
        String confirmationEmail) {
}
