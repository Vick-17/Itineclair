package fr.itineclair.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;

import fr.itineclair.identity.AccountPrincipal;
import fr.itineclair.identity.InvalidCredentialsException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class SessionAuthenticationServiceTest {

    private static final String RAW_PASSWORD =
            "une phrase de passe de test suffisamment longue";

    @Mock private AuthenticationManager authenticationManager;
    @Mock private SessionAuthenticationStrategy sessionAuthenticationStrategy;
    @Mock private SecurityContextRepository securityContextRepository;
    @Mock private SecurityContextHolderStrategy securityContextHolderStrategy;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private SecurityContext securityContext;

    private SessionAuthenticationService sessionAuthenticationService;

    @BeforeEach
    void setUp() {
        sessionAuthenticationService = new SessionAuthenticationService(
                authenticationManager,
                sessionAuthenticationStrategy,
                securityContextRepository,
                securityContextHolderStrategy);
    }

    @Test
    void authenticatesAndPersistsSecurityContext() {
        AccountPrincipal principal = mock(AccountPrincipal.class);
        Authentication authenticated = mock(Authentication.class);
        given(authenticated.getPrincipal()).willReturn(principal);
        given(authenticationManager.authenticate(any(Authentication.class)))
                .willReturn(authenticated);
        given(securityContextHolderStrategy.createEmptyContext())
                .willReturn(securityContext);

        AccountPrincipal result = sessionAuthenticationService.authenticate(
                "  Victor@Example.Test  ", RAW_PASSWORD, request, response);

        ArgumentCaptor<Authentication> authenticationRequest =
                ArgumentCaptor.forClass(Authentication.class);
        InOrder order = inOrder(
                authenticationManager,
                sessionAuthenticationStrategy,
                securityContext,
                securityContextHolderStrategy,
                securityContextRepository);
        order.verify(authenticationManager).authenticate(authenticationRequest.capture());
        order.verify(sessionAuthenticationStrategy)
                .onAuthentication(authenticated, request, response);
        order.verify(securityContext).setAuthentication(authenticated);
        order.verify(securityContextHolderStrategy).setContext(securityContext);
        order.verify(securityContextRepository)
                .saveContext(securityContext, request, response);

        assertThat(authenticationRequest.getValue().getPrincipal())
                .isEqualTo("victor@example.test");
        assertThat(authenticationRequest.getValue().getCredentials())
                .isEqualTo(RAW_PASSWORD);
        assertThat(result).isSameAs(principal);
    }

    @Test
    void convertsRejectedAuthenticationIntoGenericDomainError() {
        given(authenticationManager.authenticate(any(Authentication.class)))
                .willThrow(new BadCredentialsException("Bad credentials"));
        assertThatThrownBy(() -> sessionAuthenticationService.authenticate(
                "victor@example.test", RAW_PASSWORD, request, response))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageNotContaining("victor@example.test")
                .hasMessageNotContaining(RAW_PASSWORD);
        verifyNoInteractions(
                sessionAuthenticationStrategy,
                securityContextRepository,
                securityContextHolderStrategy);
    }

    @Test
    void preservesAuthenticationInfrastructureFailure() {
        AuthenticationServiceException outage =
                new AuthenticationServiceException("Database unavailable");
        given(authenticationManager.authenticate(any(Authentication.class)))
                .willThrow(outage);
        assertThatThrownBy(() -> sessionAuthenticationService.authenticate(
                "victor@example.test", RAW_PASSWORD, request, response))
                .isSameAs(outage);
        verifyNoInteractions(
                sessionAuthenticationStrategy,
                securityContextRepository,
                securityContextHolderStrategy);
    }

    @Test
    void refusesUnexpectedPrincipalBeforeCreatingSession() {
        Authentication authenticated = mock(Authentication.class);
        given(authenticated.getPrincipal()).willReturn("unexpected-principal");
        given(authenticationManager.authenticate(any(Authentication.class)))
                .willReturn(authenticated);
        assertThatThrownBy(() -> sessionAuthenticationService.authenticate(
                "victor@example.test", RAW_PASSWORD, request, response))
                .isInstanceOf(AuthenticationServiceException.class);
        verifyNoInteractions(
                sessionAuthenticationStrategy,
                securityContextRepository,
                securityContextHolderStrategy);
    }
}
