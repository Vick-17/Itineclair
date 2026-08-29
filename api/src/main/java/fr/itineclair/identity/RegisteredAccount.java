package fr.itineclair.identity;

import java.time.Instant;
import java.util.UUID;

public record RegisteredAccount(
    UUID id,
    String email,
    Instant createdAt
) {
    static RegisteredAccount from(UserAccount account) {
        return new RegisteredAccount(
            account.id(),
            account.email(),
            account.createdAt()
        );
    }
}
