package fr.itineclair.privacy.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request
        .SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.OutputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import fr.itineclair.identity.AccountPrincipal;
import fr.itineclair.identity.AccountRegistrationService;
import fr.itineclair.identity.api.AuthController;
import fr.itineclair.identity.api.IdentityExceptionHandler;
import fr.itineclair.privacy.AccountDataExportService;
import fr.itineclair.privacy.AccountDeletionService;
import fr.itineclair.privacy.AccountExportSnapshot;
import fr.itineclair.privacy.InvalidCurrentPasswordException;
import fr.itineclair.privacy.PreparedAccountExport;
import fr.itineclair.privacy.PrivacyWebConfiguration;
import fr.itineclair.security.AccountSessionRegistry;
import fr.itineclair.security.LoginAttemptLimiter;
import fr.itineclair.security.LoginRateLimitExceededException;
import fr.itineclair.security.SecurityConfiguration;
import fr.itineclair.security.SessionAuthenticationService;
import jakarta.servlet.http.Cookie;

@WebMvcTest(controllers = {
        AccountPrivacyController.class,
        AuthController.class
})
@Import({
        SecurityConfiguration.class,
        PrivacyWebConfiguration.class,
        PrivacyExceptionHandler.class,
        IdentityExceptionHandler.class
})
class AccountPrivacyControllerTest {

    private static final UUID ACCOUNT_ID =
            UUID.fromString("936dd470-a45c-46fa-a0bd-94a76e4b836a");
    private static final Instant NOW =
            Instant.parse("2026-09-01T12:00:00Z");
    private static final String PASSWORD =
            "une phrase de passe de test suffisamment longue";

    @Autowired private MockMvc mockMvc;

    @MockitoBean private AccountDataExportService accountDataExportService;
    @MockitoBean private AccountDeletionService accountDeletionService;
    @MockitoBean private AccountRegistrationService accountRegistrationService;
    @MockitoBean private SessionAuthenticationService sessionAuthenticationService;
    @MockitoBean private LoginAttemptLimiter loginAttemptLimiter;
    @MockitoBean private AccountSessionRegistry accountSessionRegistry;
    @MockitoBean private UserDetailsService userDetailsService;

    @Test
    void accountActionsRequireAuthentication() throws Exception {
        mockMvc.perform(withCsrf(
                        post("/account/export")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(exportRequest())))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(accountDataExportService);
    }

    @Test
    void exportRequiresCsrfEvenForAnAuthenticatedAccount()
            throws Exception {
        mockMvc.perform(post("/account/export")
                        .with(authentication(accountAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(exportRequest()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(accountDataExportService);
    }

    @Test
    void streamsAnUncachedAttachmentAfterReauthentication()
            throws Exception {
        PreparedAccountExport prepared = preparedExport();
        given(accountDataExportService.prepareExport(
                any(AccountPrincipal.class),
                eq(PASSWORD)))
                .willReturn(prepared);
        willAnswer(invocation -> {
            OutputStream output = invocation.getArgument(1);
            output.write("ZIP".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return null;
        }).given(accountDataExportService).writeExport(
                eq(prepared),
                any(OutputStream.class));

        MvcResult asyncResult = mockMvc.perform(withCsrf(
                        post("/account/export")
                                .with(authentication(accountAuthentication()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(exportRequest())))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(asyncResult))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.CACHE_CONTROL,
                        containsString("no-store")))
                .andExpect(header().string(
                        HttpHeaders.CONTENT_DISPOSITION,
                        containsString(prepared.filename())))
                .andExpect(header().string(
                        HttpHeaders.CONTENT_TYPE,
                        containsString("application/zip")))
                .andExpect(content().bytes("ZIP".getBytes(
                        java.nio.charset.StandardCharsets.UTF_8)));
    }

    @Test
    void invalidCurrentPasswordIsGenericAndCountsAsAFailure()
            throws Exception {
        given(accountDataExportService.prepareExport(
                any(AccountPrincipal.class),
                eq(PASSWORD)))
                .willThrow(new InvalidCurrentPasswordException());

        mockMvc.perform(withCsrf(
                        post("/account/export")
                                .with(authentication(accountAuthentication()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(exportRequest())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code")
                        .value("invalid_current_password"))
                .andExpect(content().string(not(containsString(PASSWORD))));

        verify(loginAttemptLimiter, never())
                .authenticationSucceeded(any());
    }

    @Test
    void deletionRequiresCsrfAndThenClearsBothCookies()
            throws Exception {
        mockMvc.perform(delete("/account")
                        .with(authentication(accountAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deleteRequest()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(accountDeletionService);

        MvcResult result = mockMvc.perform(withCsrf(
                        delete("/account")
                                .with(authentication(accountAuthentication()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(deleteRequest())))
                .andExpect(status().isNoContent())
                .andExpect(header().string(
                        HttpHeaders.CACHE_CONTROL,
                        containsString("no-store")))
                .andReturn();

        List<String> cookies = result.getResponse().getHeaders(
                HttpHeaders.SET_COOKIE);
        org.assertj.core.api.Assertions.assertThat(cookies)
                .anySatisfy(cookie ->
                        org.assertj.core.api.Assertions.assertThat(cookie)
                                .contains("ITINECLAIR_SESSION=")
                                .contains("Path=/api")
                                .contains("Max-Age=0")
                                .contains("HttpOnly"))
                .anySatisfy(cookie ->
                        org.assertj.core.api.Assertions.assertThat(cookie)
                                .contains("XSRF-TOKEN=")
                                .contains("Path=/")
                                .contains("Max-Age=0"));

        verify(accountDeletionService).deleteAccount(
                any(AccountPrincipal.class),
                eq(PASSWORD),
                eq("victor@example.test"));
        verify(accountSessionRegistry).invalidateAll(ACCOUNT_ID);
    }

    @Test
    void sensitiveActionRateLimitHasItsOwnContract()
            throws Exception {
        given(loginAttemptLimiter.beforeAuthentication(
                eq("victor@example.test"),
                any()))
                .willThrow(new LoginRateLimitExceededException(
                        Duration.ofSeconds(42)));

        mockMvc.perform(withCsrf(
                        post("/account/export")
                                .with(authentication(accountAuthentication()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(exportRequest())))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string(HttpHeaders.RETRY_AFTER, "42"))
                .andExpect(jsonPath("$.code")
                        .value("account_action_rate_limited"));

        verifyNoInteractions(accountDataExportService);
    }

    private PreparedAccountExport preparedExport() {
        return new PreparedAccountExport(
                "itineclair-export-20260901T120000Z.zip",
                NOW,
                new AccountExportSnapshot(
                        new AccountExportSnapshot.Account(
                                ACCOUNT_ID,
                                "victor@example.test",
                                NOW.minusSeconds(86_400)),
                        List.of()));
    }

    private String exportRequest() {
        return """
                {
                  "currentPassword": "une phrase de passe de test suffisamment longue"
                }
                """;
    }

    private String deleteRequest() {
        return """
                {
                  "currentPassword": "une phrase de passe de test suffisamment longue",
                  "confirmationEmail": "victor@example.test"
                }
                """;
    }

    private MockHttpServletRequestBuilder withCsrf(
            MockHttpServletRequestBuilder request)
            throws Exception {
        Cookie csrfCookie = mockMvc.perform(get("/auth/csrf"))
                .andExpect(status().isNoContent())
                .andReturn()
                .getResponse()
                .getCookie("XSRF-TOKEN");

        if (csrfCookie == null) {
            throw new AssertionError("Missing XSRF-TOKEN test cookie.");
        }

        return request
                .cookie(csrfCookie)
                .header("X-XSRF-TOKEN", csrfCookie.getValue());
    }

    private Authentication accountAuthentication() {
        AccountPrincipal principal = mock(AccountPrincipal.class);
        given(principal.id()).willReturn(ACCOUNT_ID);
        given(principal.email()).willReturn("victor@example.test");

        return org.springframework.security.authentication
                .UsernamePasswordAuthenticationToken
                .authenticated(
                        principal,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }
}
