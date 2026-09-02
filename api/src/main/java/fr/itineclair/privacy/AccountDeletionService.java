package fr.itineclair.privacy;

import java.util.Locale;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fr.itineclair.identity.AccountPrincipal;
import fr.itineclair.security.CurrentPasswordVerifier;

@Service
public class AccountDeletionService {

    private final AccountDataStore accountDataStore;
    private final CurrentPasswordVerifier currentPasswordVerifier;

    public AccountDeletionService(
            AccountDataStore accountDataStore,
            CurrentPasswordVerifier currentPasswordVerifier) {
        this.accountDataStore = accountDataStore;
        this.currentPasswordVerifier = currentPasswordVerifier;
    }

    @Transactional
    public void deleteAccount(
            AccountPrincipal principal,
            String rawPassword,
            String confirmationEmail) {
        Objects.requireNonNull(principal, "principal is required.");

        if (!currentPasswordVerifier.matches(principal, rawPassword)) {
            throw new InvalidCurrentPasswordException();
        }

        String normalizedConfirmation = confirmationEmail
                .strip()
                .toLowerCase(Locale.ROOT);

        if (!principal.email().equals(normalizedConfirmation)) {
            throw new AccountConfirmationMismatchException();
        }

        if (accountDataStore.deleteAccount(principal.id()) != 1) {
            throw new AccountDataUnavailableException();
        }
    }
}
