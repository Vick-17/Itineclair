package fr.itineclair.identity.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterAccountRequest(

    @NotBlank(message = "L'adresse email ne peut pas être vide")
    @Email(message = "L'adresse email doit être valide")
    @Size(max = 254, message = "L'adresse email ne doit pas dépasser 254 caractères")
    String email,

    @NotBlank(message = "Le mot de passe est obligatoire.")
    @Size(min = 15, max = 128, message = "Le mot de passe doit contenir au moins 15 caractères et au maximum 128 caractères")
    String password
) {
}
