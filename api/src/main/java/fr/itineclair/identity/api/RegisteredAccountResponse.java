package fr.itineclair.identity.api;

import java.time.Instant;
import java.util.UUID;

import fr.itineclair.identity.RegisteredAccount;

public record RegisteredAccountResponse(
    UUID id,
    String email,
    Instant createdAt
) {

    static RegisteredAccountResponse from(RegisteredAccount account) {
        return new RegisteredAccountResponse(
            account.id(),
            account.email(),
            account.createdAt()
        );
    }
}
