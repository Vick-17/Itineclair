package fr.itineclair.sharing.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import fr.itineclair.analysis.AnalysisSourceSnapshot;
import fr.itineclair.analysis.RuleReviewStatus;
import fr.itineclair.analysis.TrackAnalysis;
import fr.itineclair.identity.AccountPrincipal;
import fr.itineclair.identity.AccountRegistrationService;
import fr.itineclair.identity.api.AuthController;
import fr.itineclair.identity.api.IdentityExceptionHandler;
import fr.itineclair.security.LoginAttemptLimiter;
import fr.itineclair.security.SecurityConfiguration;
import fr.itineclair.security.SessionAuthenticationService;
import fr.itineclair.sharing.CreatedTrackShare;
import fr.itineclair.sharing.SharedReportNotFoundException;
import fr.itineclair.sharing.SharedTrackReport;
import fr.itineclair.sharing.TrackShareService;
import fr.itineclair.sharing.TrackShareStatus;
import fr.itineclair.track.TrackFacts;
import fr.itineclair.track.TrackSummary;
import fr.itineclair.track.api.TrackExceptionHandler;
import jakarta.servlet.http.Cookie;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        TrackShareController.class,
        PublicSharedReportController.class,
        AuthController.class
})
@Import({
        SecurityConfiguration.class,
        TrackShareExceptionHandler.class,
        IdentityExceptionHandler.class,
        TrackExceptionHandler.class
})
class TrackShareControllerTest {

    private static final UUID ACCOUNT_ID =
            UUID.fromString("936dd470-a45c-46fa-a0bd-94a76e4b836a");
    private static final UUID TRACK_ID =
            UUID.fromString("ce7b58c7-d6f4-4a26-8bad-68265bdb7bbf");
    private static final String TOKEN = "A".repeat(43);
    private static final Instant NOW =
            Instant.parse("2026-09-01T10:00:00Z");
    private static final Instant EXPIRES_AT =
            Instant.parse("2026-09-08T10:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TrackShareService trackShareService;

    @MockitoBean
    private AccountRegistrationService accountRegistrationService;

    @MockitoBean
    private SessionAuthenticationService sessionAuthenticationService;

    @MockitoBean
    private LoginAttemptLimiter loginAttemptLimiter;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void publicReportIsAnonymousUncachedAndPrivacyReduced()
            throws Exception {
        given(trackShareService.getPublicReport(TOKEN))
                .willReturn(sharedReport(EXPIRES_AT));

        mockMvc.perform(get("/shared-report")
                        .header(
                                PublicSharedReportController.TOKEN_HEADER,
                                TOKEN))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        "Cache-Control",
                        containsString("no-store")))
                .andExpect(header().string(
                        "Referrer-Policy",
                        "no-referrer"))
                .andExpect(header().string(
                        "X-Robots-Tag",
                        "noindex, nofollow"))
                .andExpect(jsonPath("$.shareVersion").value(1))
                .andExpect(jsonPath("$.expiresAt")
                        .value("2026-09-08T10:00:00Z"))
                .andExpect(jsonPath("$.track.facts.distanceMeters")
                        .value(12_450.5))
                .andExpect(jsonPath("$.analysis.ruleSetVersion")
                        .value(1))
                .andExpect(jsonPath("$.privacy.excludedData.length()")
                        .value(4))
                .andExpect(content().string(
                        not(containsString("maison-secrete.gpx"))))
                .andExpect(content().string(
                        not(containsString("Départ maison"))))
                .andExpect(content().string(
                        not(containsString(TOKEN))))
                .andExpect(jsonPath("$.feedback").doesNotExist());
    }

    @Test
    void missingExpiredAndRevokedSharesUseOnePublicNotFoundResponse()
            throws Exception {
        given(trackShareService.getPublicReport(null))
                .willThrow(new SharedReportNotFoundException());

        mockMvc.perform(get("/shared-report"))
                .andExpect(status().isNotFound())
                .andExpect(header().string(
                        "Cache-Control",
                        containsString("no-store")))
                .andExpect(header().string(
                        "Referrer-Policy",
                        "no-referrer"))
                .andExpect(header().string(
                        "X-Robots-Tag",
                        "noindex, nofollow"))
                .andExpect(jsonPath("$.code")
                        .value("shared_report_not_found"))
                .andExpect(jsonPath("$.detail")
                        .value("Ce partage n’existe plus ou n’est pas accessible."));
    }

    @Test
    void ownerMustAuthenticateBeforeReadingShareStatus()
            throws Exception {
        mockMvc.perform(get(
                        "/tracks/{trackId}/share",
                        TRACK_ID))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(trackShareService);
    }

    @Test
    void ownerCanPreviewExactlyThePublicContract()
            throws Exception {
        given(trackShareService.preview(ACCOUNT_ID, TRACK_ID))
                .willReturn(sharedReport(null));

        mockMvc.perform(get(
                        "/tracks/{trackId}/share/preview",
                        TRACK_ID)
                        .with(authentication(accountAuthentication())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expiresAt").doesNotExist())
                .andExpect(jsonPath("$.track.facts.distanceMeters")
                        .value(12_450.5))
                .andExpect(content().string(
                        not(containsString("maison-secrete.gpx"))));
    }

    @Test
    void ownerCreatesASevenDayShareWithCsrfProtection()
            throws Exception {
        given(trackShareService.createOrRotate(
                ACCOUNT_ID,
                TRACK_ID,
                7))
                .willReturn(new CreatedTrackShare(
                        TOKEN,
                        EXPIRES_AT,
                        NOW));

        mockMvc.perform(postWithCsrf(
                        "/tracks/{trackId}/share",
                        TRACK_ID)
                        .with(authentication(accountAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "durationDays": 7
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Cache-Control",
                        containsString("no-store")))
                .andExpect(jsonPath("$.token").value(TOKEN))
                .andExpect(jsonPath("$.expiresAt")
                        .value("2026-09-08T10:00:00Z"));
    }

    @Test
    void ownerCannotCreateAShareWithoutCsrfToken()
            throws Exception {
        mockMvc.perform(post(
                        "/tracks/{trackId}/share",
                        TRACK_ID)
                        .with(authentication(accountAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "durationDays": 7
                                }
                                """))
                .andExpect(status().isForbidden());

        verifyNoInteractions(trackShareService);
    }

    @Test
    void ownerCanRevokeImmediatelyWithCsrfProtection()
            throws Exception {
        mockMvc.perform(deleteWithCsrf(
                        "/tracks/{trackId}/share",
                        TRACK_ID)
                        .with(authentication(accountAuthentication())))
                .andExpect(status().isNoContent());

        verify(trackShareService).revoke(ACCOUNT_ID, TRACK_ID);
    }

    private SharedTrackReport sharedReport(Instant expiresAt) {
        TrackSummary track = new TrackSummary(
                TRACK_ID,
                "Départ maison",
                "maison-secrete.gpx",
                2,
                120,
                120,
                TrackFacts.CURRENT_VERSION,
                12_450.5,
                850.0,
                810.0,
                1_020.0,
                1_870.0,
                18.4,
                21.2,
                NOW);
        TrackAnalysis analysis = new TrackAnalysis(
                1,
                RuleReviewStatus.PROTOTYPE_AWAITING_EXPERT_REVIEW,
                NOW,
                new AnalysisSourceSnapshot(1, null, null),
                List.of(),
                List.of(),
                List.of("Limite connue."));

        return new SharedTrackReport(
                1,
                expiresAt,
                track,
                null,
                analysis);
    }

    private MockHttpServletRequestBuilder postWithCsrf(
            String path,
            Object... uriVariables) throws Exception {
        return withCsrf(post(path, uriVariables));
    }

    private MockHttpServletRequestBuilder deleteWithCsrf(
            String path,
            Object... uriVariables) throws Exception {
        return withCsrf(delete(path, uriVariables));
    }

    private MockHttpServletRequestBuilder withCsrf(
            MockHttpServletRequestBuilder request) throws Exception {
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

        return org.springframework.security.authentication
                .UsernamePasswordAuthenticationToken
                .authenticated(
                        principal,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }
}
