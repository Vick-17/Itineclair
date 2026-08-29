package fr.itineclair.identity.api;

import fr.itineclair.identity.AccountRegistrationService;
import fr.itineclair.identity.AccountPrincipal;
import fr.itineclair.identity.AccountRole;
import fr.itineclair.identity.EmailAlreadyUsedException;
import fr.itineclair.identity.InvalidCredentialsException;
import fr.itineclair.identity.RegisteredAccount;
import fr.itineclair.security.LoginAttemptLimiter;
import fr.itineclair.security.LoginRateLimitExceededException;
import fr.itineclair.security.SecurityConfiguration;
import fr.itineclair.security.SessionAuthenticationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.servlet.http.Cookie;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(SecurityConfiguration.class)
class AuthControllerTest {

    private static final String PASSWORD = "une phrase de passe de test suffisamment longue";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountRegistrationService accountRegistrationService;

    @MockitoBean
    private SessionAuthenticationService sessionAuthenticationService;

    @MockitoBean
    private LoginAttemptLimiter loginAttemptLimiter;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void createsCsrfCookie() throws Exception {
        mockMvc.perform(get("/auth/csrf"))
                .andExpect(status().isNoContent())
                .andExpect(cookie().exists("XSRF-TOKEN"));
    }

    @Test
    void registersAValidAccount() throws Exception {
        UUID accountId = UUID.fromString("936dd470-a45c-46fa-a0bd-94a76e4b836a");

        Instant createdAt = Instant.parse("2026-08-27T10:00:00Z");

        given(accountRegistrationService.register(
                "victor@example.test",
                PASSWORD))
                .willReturn(new RegisteredAccount(
                        accountId,
                        "victor@example.test",
                        createdAt));

        mockMvc.perform(postWithCsrf("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "email": "victor@example.test",
                          "password": "une phrase de passe de test suffisamment longue"
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id")
                        .value(accountId.toString()))
                .andExpect(jsonPath("$.email")
                        .value("victor@example.test"))
                .andExpect(jsonPath("$.createdAt")
                        .value("2026-08-27T10:00:00Z"));
    }

    @Test
    void rejectsRegistrationWithoutCsrfToken() throws Exception {
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "email": "victor@example.test",
                          "password": "une phrase de passe de test suffisamment longue"
                        }
                        """))
                .andExpect(status().isForbidden());

        verifyNoInteractions(accountRegistrationService);
    }

    @Test
    void rejectsInvalidPasswordWithoutReturningItsValue() throws Exception {
        mockMvc.perform(postWithCsrf("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "email": "victor@example.test",
                          "password": "trop court"
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("validation_failed"))
                .andExpect(jsonPath("$.violations[0].field")
                        .value("password"))
                .andExpect(content().string(
                        not(containsString("trop court"))));
    }

    @Test
    void returnsConflictWhenEmailAlreadyExists() throws Exception {
        given(accountRegistrationService.register(
                "victor@example.test",
                PASSWORD))
                .willThrow(new EmailAlreadyUsedException());

        mockMvc.perform(postWithCsrf("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "email": "victor@example.test",
                          "password": "une phrase de passe de test suffisamment longue"
                        }
                        """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code")
                        .value("email_already_used"));
    }

    @Test
    void logsInWithAValidAccount() throws Exception {
        UUID accountId = UUID.fromString("936dd470-a45c-46fa-a0bd-94a76e4b836a");
        AccountPrincipal principal = accountPrincipal(accountId, "victor@example.test");
        LoginAttemptLimiter.Permit permit =
                mock(LoginAttemptLimiter.Permit.class);
        given(loginAttemptLimiter.beforeAuthentication(
                eq("victor@example.test"),
                any()))
                .willReturn(permit);
        given(sessionAuthenticationService.authenticate(
                eq("victor@example.test"), eq(PASSWORD), any(), any()))
                .willReturn(principal);

        mockMvc.perform(postWithCsrf("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "email": "victor@example.test",
                          "password": "une phrase de passe de test suffisamment longue"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(accountId.toString()))
                .andExpect(jsonPath("$.email").value("victor@example.test"))
                .andExpect(jsonPath("$.role").value("USER"));

        verify(loginAttemptLimiter)
                .authenticationSucceeded(permit);
    }

    @Test
    void returnsSameGenericErrorForInvalidCredentials() throws Exception {
        LoginAttemptLimiter.Permit permit =
                mock(LoginAttemptLimiter.Permit.class);
        given(loginAttemptLimiter.beforeAuthentication(
                eq("unknown@example.test"),
                any()))
                .willReturn(permit);
        given(sessionAuthenticationService.authenticate(
                eq("unknown@example.test"), eq(PASSWORD), any(), any()))
                .willThrow(new InvalidCredentialsException());

        mockMvc.perform(postWithCsrf("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "email": "unknown@example.test",
                          "password": "une phrase de passe de test suffisamment longue"
                        }
                        """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("invalid_credentials"))
                .andExpect(content().string(not(containsString("unknown@example.test"))))
                .andExpect(content().string(not(containsString(PASSWORD))));

        verify(loginAttemptLimiter, never())
                .authenticationSucceeded(permit);
        verify(loginAttemptLimiter, never())
                .authenticationUnavailable(permit);
    }

    @Test
    void returnsRetryAfterWhenLoginIsRateLimited() throws Exception {
        given(loginAttemptLimiter.beforeAuthentication(
                eq("victor@example.test"),
                any()))
                .willThrow(new LoginRateLimitExceededException(
                        Duration.ofSeconds(42)));

        mockMvc.perform(postWithCsrf("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "email": "victor@example.test",
                          "password": "une phrase de passe de test suffisamment longue"
                        }
                        """))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "42"))
                .andExpect(jsonPath("$.code").value("login_rate_limited"))
                .andExpect(content().string(not(containsString("victor@example.test"))))
                .andExpect(content().string(not(containsString(PASSWORD))));

        verifyNoInteractions(sessionAuthenticationService);
    }

    @Test
    void releasesReservedIdentityAttemptsWhenAuthenticationIsUnavailable()
            throws Exception {
        LoginAttemptLimiter.Permit permit =
                mock(LoginAttemptLimiter.Permit.class);
        given(loginAttemptLimiter.beforeAuthentication(
                eq("victor@example.test"),
                any()))
                .willReturn(permit);
        given(sessionAuthenticationService.authenticate(
                eq("victor@example.test"), eq(PASSWORD), any(), any()))
                .willThrow(new AuthenticationServiceException(
                        "Database unavailable"));

        mockMvc.perform(postWithCsrf("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "email": "victor@example.test",
                          "password": "une phrase de passe de test suffisamment longue"
                        }
                        """))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code")
                        .value("authentication_unavailable"));

        verify(loginAttemptLimiter)
                .authenticationUnavailable(permit);
        verify(loginAttemptLimiter, never())
                .authenticationSucceeded(permit);
    }

    @Test
    void returnsCurrentAuthenticatedAccount() throws Exception {
        UUID accountId = UUID.fromString("936dd470-a45c-46fa-a0bd-94a76e4b836a");
        AccountPrincipal principal = accountPrincipal(accountId, "victor@example.test");
        mockMvc.perform(get("/auth/me").with(authentication(authenticated(principal))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(accountId.toString()))
                .andExpect(jsonPath("$.email").value("victor@example.test"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void rejectsCurrentAccountWhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logsOutAndClearsSessionAndCsrfCookies() throws Exception {
        AccountPrincipal principal = accountPrincipal(
                UUID.fromString("936dd470-a45c-46fa-a0bd-94a76e4b836a"),
                "victor@example.test");
        Authentication authentication = authenticated(principal);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                new SecurityContextImpl(authentication));

        mockMvc.perform(postWithCsrf("/auth/logout").session(session))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge("ITINECLAIR_SESSION", 0))
                .andExpect(cookie().maxAge("XSRF-TOKEN", 0));
    }

    private MockHttpServletRequestBuilder postWithCsrf(String path) throws Exception {
        Cookie csrfCookie = mockMvc.perform(get("/auth/csrf"))
                .andExpect(status().isNoContent())
                .andReturn()
                .getResponse()
                .getCookie("XSRF-TOKEN");

        return post(path)
                .cookie(csrfCookie)
                .header("X-XSRF-TOKEN", csrfCookie.getValue());
    }

    private AccountPrincipal accountPrincipal(UUID id, String email) {
        AccountPrincipal principal = mock(AccountPrincipal.class);
        given(principal.id()).willReturn(id);
        given(principal.email()).willReturn(email);
        given(principal.role()).willReturn(AccountRole.USER);
        return principal;
    }

    private Authentication authenticated(AccountPrincipal principal) {
        return UsernamePasswordAuthenticationToken.authenticated(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }
}
