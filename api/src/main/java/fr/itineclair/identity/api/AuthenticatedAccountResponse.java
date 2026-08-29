package fr.itineclair.identity.api;

import java.util.UUID;

import fr.itineclair.identity.AccountPrincipal;
import fr.itineclair.identity.AccountRole;

public record AuthenticatedAccountResponse(
        UUID id,
        String email,
        AccountRole role) {

    static AuthenticatedAccountResponse from(
            AccountPrincipal principal) {
        return new AuthenticatedAccountResponse(
                principal.id(),
                principal.email(),
                principal.role());
    }
}
