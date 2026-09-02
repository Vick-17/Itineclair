package fr.itineclair.privacy;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import fr.itineclair.identity.AccountPrincipal;
import fr.itineclair.security.CurrentPasswordVerifier;

@ExtendWith(MockitoExtension.class)
class AccountDeletionServiceTest {

    private static final UUID ACCOUNT_ID =
            UUID.fromString("936dd470-a45c-46fa-a0bd-94a76e4b836a");
    private static final String PASSWORD =
            "une phrase de passe de test suffisamment longue";

    @Mock private AccountDataStore accountDataStore;
    @Mock private CurrentPasswordVerifier currentPasswordVerifier;

    private AccountDeletionService service;
    private AccountPrincipal principal;

    @BeforeEach
    void setUp() {
        service = new AccountDeletionService(
                accountDataStore,
                currentPasswordVerifier);
        principal = mock(AccountPrincipal.class);
        lenient().when(principal.id()).thenReturn(ACCOUNT_ID);
        lenient().when(principal.email())
                .thenReturn("victor@example.test");
    }

    @Test
    void deletesTheConfirmedAccount() {
        given(currentPasswordVerifier.matches(principal, PASSWORD))
                .willReturn(true);
        given(accountDataStore.deleteAccount(ACCOUNT_ID)).willReturn(1);

        service.deleteAccount(
                principal,
                PASSWORD,
                "  Victor@Example.Test  ");

        verify(accountDataStore).deleteAccount(ACCOUNT_ID);
    }

    @Test
    void wrongPasswordDoesNotRevealOrDeleteData() {
        given(currentPasswordVerifier.matches(principal, PASSWORD))
                .willReturn(false);

        assertThatThrownBy(() -> service.deleteAccount(
                principal,
                PASSWORD,
                "victor@example.test"))
                .isInstanceOf(InvalidCurrentPasswordException.class)
                .hasMessageNotContaining(PASSWORD);

        verify(accountDataStore, never()).deleteAccount(ACCOUNT_ID);
    }

    @Test
    void mismatchedEmailDoesNotDeleteData() {
        given(currentPasswordVerifier.matches(principal, PASSWORD))
                .willReturn(true);

        assertThatThrownBy(() -> service.deleteAccount(
                principal,
                PASSWORD,
                "another@example.test"))
                .isInstanceOf(AccountConfirmationMismatchException.class);

        verify(accountDataStore, never()).deleteAccount(ACCOUNT_ID);
    }

    @Test
    void reportsAConcurrentMissingAccount() {
        given(currentPasswordVerifier.matches(principal, PASSWORD))
                .willReturn(true);
        given(accountDataStore.deleteAccount(ACCOUNT_ID)).willReturn(0);

        assertThatThrownBy(() -> service.deleteAccount(
                principal,
                PASSWORD,
                "victor@example.test"))
                .isInstanceOf(AccountDataUnavailableException.class);
    }
}
