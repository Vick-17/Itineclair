package fr.itineclair.identity.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(

        @NotBlank(message = "L’adresse e-mail est obligatoire.")
        @Email(message = "L’adresse e-mail n’est pas valide.")
        @Size(
                max = 254,
                message = "L’adresse e-mail ne peut pas dépasser 254 caractères.")
        String email,

        @NotBlank(message = "Le mot de passe est obligatoire.")
        @Size(
                max = 128,
                message = "Le mot de passe ne peut pas dépasser 128 caractères.")
        String password) {
}
