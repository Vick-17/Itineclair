package fr.itineclair.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;

import fr.itineclair.identity.AccountPrincipal;

@ExtendWith(MockitoExtension.class)
class CurrentPasswordVerifierTest {

    private static final UUID ACCOUNT_ID =
            UUID.fromString("936dd470-a45c-46fa-a0bd-94a76e4b836a");
    private static final String PASSWORD =
            "une phrase de passe de test suffisamment longue";

    @Mock
    private AuthenticationManager authenticationManager;

    @Test
    void acceptsCredentialsForTheCurrentAccountOnly() {
        AccountPrincipal current = principal(ACCOUNT_ID);
        AccountPrincipal verified = principal(ACCOUNT_ID);
        Authentication authentication = mock(Authentication.class);
        given(authentication.getPrincipal()).willReturn(verified);
        given(authenticationManager.authenticate(any(Authentication.class)))
                .willReturn(authentication);

        boolean matches = new CurrentPasswordVerifier(authenticationManager)
                .matches(current, PASSWORD);

        ArgumentCaptor<Authentication> request =
                ArgumentCaptor.forClass(Authentication.class);
        org.mockito.Mockito.verify(authenticationManager)
                .authenticate(request.capture());

        assertThat(matches).isTrue();
        assertThat(request.getValue().getPrincipal())
                .isEqualTo("victor@example.test");
        assertThat(request.getValue().getCredentials()).isEqualTo(PASSWORD);
    }

    @Test
    void rejectsBadCredentialsAndAnotherAuthenticatedAccount() {
        AccountPrincipal current = principal(ACCOUNT_ID);
        given(authenticationManager.authenticate(any(Authentication.class)))
                .willThrow(new BadCredentialsException("Rejected"));

        CurrentPasswordVerifier verifier =
                new CurrentPasswordVerifier(authenticationManager);

        assertThat(verifier.matches(current, PASSWORD)).isFalse();

        Authentication otherAuthentication = mock(Authentication.class);
        AccountPrincipal otherPrincipal = principal(UUID.fromString(
                "ce7b58c7-d6f4-4a26-8bad-68265bdb7bbf"));
        given(otherAuthentication.getPrincipal()).willReturn(otherPrincipal);
        given(authenticationManager.authenticate(any(Authentication.class)))
                .willReturn(otherAuthentication);

        assertThat(verifier.matches(current, PASSWORD)).isFalse();
    }

    @Test
    void preservesAuthenticationInfrastructureFailures() {
        AuthenticationServiceException outage =
                new AuthenticationServiceException("Unavailable");
        given(authenticationManager.authenticate(any(Authentication.class)))
                .willThrow(outage);

        assertThatThrownBy(() ->
                new CurrentPasswordVerifier(authenticationManager)
                        .matches(principal(ACCOUNT_ID), PASSWORD))
                .isSameAs(outage);
    }

    private AccountPrincipal principal(UUID id) {
        AccountPrincipal principal = mock(AccountPrincipal.class);
        lenient().when(principal.id()).thenReturn(id);
        lenient().when(principal.email())
                .thenReturn("victor@example.test");
        return principal;
    }
}
