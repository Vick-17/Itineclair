package fr.itineclair.feedback.api;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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

import fr.itineclair.feedback.ConditionsComparison;
import fr.itineclair.feedback.FeedbackIssue;
import fr.itineclair.feedback.FeedbackOutcome;
import fr.itineclair.feedback.InvalidTrackFeedbackException;
import fr.itineclair.feedback.TrackFeedbackCommand;
import fr.itineclair.feedback.TrackFeedbackService;
import fr.itineclair.feedback.TrackFeedbackView;
import fr.itineclair.identity.AccountPrincipal;
import fr.itineclair.identity.AccountRegistrationService;
import fr.itineclair.identity.api.AuthController;
import fr.itineclair.identity.api.IdentityExceptionHandler;
import fr.itineclair.security.LoginAttemptLimiter;
import fr.itineclair.security.SecurityConfiguration;
import fr.itineclair.security.SessionAuthenticationService;
import fr.itineclair.track.TrackNotFoundException;
import fr.itineclair.track.api.TrackExceptionHandler;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;

@WebMvcTest(controllers = {
        TrackFeedbackController.class,
        AuthController.class
})
@Import({
        SecurityConfiguration.class,
        TrackFeedbackExceptionHandler.class,
        IdentityExceptionHandler.class,
        TrackExceptionHandler.class
})
class TrackFeedbackControllerTest {

    private static final UUID ACCOUNT_ID = UUID.fromString("936dd470-a45c-46fa-a0bd-94a76e4b836a");
    private static final UUID TRACK_ID = UUID.fromString("ce7b58c7-d6f4-4a26-8bad-68265bdb7bbf");
    private static final Instant CREATED_AT = Instant.parse("2026-08-31T12:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-08-31T12:30:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TrackFeedbackService trackFeedbackService;

    @MockitoBean
    private AccountRegistrationService accountRegistrationService;

    @MockitoBean
    private SessionAuthenticationService sessionAuthenticationService;

    @MockitoBean
    private LoginAttemptLimiter loginAttemptLimiter;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void rejectsAnonymousAccess() throws Exception {
        mockMvc.perform(get("/tracks/{trackId}/feedback", TRACK_ID))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(trackFeedbackService);
    }

    @Test
    void returnsAnExplicitEmptyFeedback() throws Exception {
        given(trackFeedbackService.get(ACCOUNT_ID, TRACK_ID)).willReturn(Optional.empty());

        mockMvc.perform(get("/tracks/{trackId}/feedback", TRACK_ID)
                        .with(authentication(accountAuthentication())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recorded").value(false))
                .andExpect(jsonPath("$.trackId").value(TRACK_ID.toString()))
                .andExpect(jsonPath("$.observedIssues").isEmpty())
                .andExpect(jsonPath("$.outcome").doesNotExist());
    }

    @Test
    void savesStructuredFeedbackWithCsrfProtection() throws Exception {
        TrackFeedbackCommand expectedCommand = validCommand();
        given(trackFeedbackService.save(ACCOUNT_ID, TRACK_ID, expectedCommand))
                .willReturn(feedbackView());

        mockMvc.perform(putWithCsrf("/tracks/{trackId}/feedback", TRACK_ID)
                        .with(authentication(accountAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recorded").value(true))
                .andExpect(jsonPath("$.outcome").value("COMPLETED_WITH_CHANGES"))
                .andExpect(jsonPath("$.actualDurationMinutes").value(405))
                .andExpect(jsonPath("$.perceivedEffort").value(4))
                .andExpect(jsonPath("$.conditionsComparison").value("WORSE_THAN_EXPECTED"))
                .andExpect(jsonPath("$.observedIssues[0]").value("WEATHER"))
                .andExpect(jsonPath("$.observedIssues[1]").value("FATIGUE"))
                .andExpect(jsonPath("$.updatedAt").value("2026-08-31T12:30:00Z"));

        verify(trackFeedbackService).save(ACCOUNT_ID, TRACK_ID, expectedCommand);
    }

    @Test
    void rejectsSaveWithoutCsrfToken() throws Exception {
        mockMvc.perform(put("/tracks/{trackId}/feedback", TRACK_ID)
                        .with(authentication(accountAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(trackFeedbackService);
    }

    @Test
    void rejectsDeleteWithoutCsrfToken() throws Exception {
        mockMvc.perform(delete("/tracks/{trackId}/feedback", TRACK_ID)
                        .with(authentication(accountAuthentication())))
                .andExpect(status().isForbidden());

        verifyNoInteractions(trackFeedbackService);
    }

    @Test
    void rejectsInvalidDurationBeforeCallingTheService() throws Exception {
        mockMvc.perform(putWithCsrf("/tracks/{trackId}/feedback", TRACK_ID)
                        .with(authentication(accountAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "outcome": "COMPLETED_AS_PLANNED",
                                  "actualDurationMinutes": 1441,
                                  "perceivedEffort": null,
                                  "conditionsComparison": "AS_EXPECTED",
                                  "observedIssues": []
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"))
                .andExpect(jsonPath("$.violations[0].field").value("actualDurationMinutes"));

        verifyNoInteractions(trackFeedbackService);
    }

    @Test
    void returnsAControlledSemanticValidationProblem() throws Exception {
        TrackFeedbackCommand invalidCommand = new TrackFeedbackCommand(
                FeedbackOutcome.NOT_STARTED,
                30,
                null,
                ConditionsComparison.NOT_COMPARED,
                Set.of());
        given(trackFeedbackService.save(ACCOUNT_ID, TRACK_ID, invalidCommand))
                .willThrow(new InvalidTrackFeedbackException(
                        "Une sortie non démarrée ne peut pas contenir d’observations terrain."));

        mockMvc.perform(putWithCsrf("/tracks/{trackId}/feedback", TRACK_ID)
                        .with(authentication(accountAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "outcome": "NOT_STARTED",
                                  "actualDurationMinutes": 30,
                                  "perceivedEffort": null,
                                  "conditionsComparison": "NOT_COMPARED",
                                  "observedIssues": []
                                }
                                """))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("invalid_track_feedback"));
    }

    @Test
    void rejectsAnUnknownEnumWithoutLeakingThePayload() throws Exception {
        mockMvc.perform(putWithCsrf("/tracks/{trackId}/feedback", TRACK_ID)
                        .with(authentication(accountAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "outcome": "DANGEROUS_VALUE",
                                  "actualDurationMinutes": null,
                                  "perceivedEffort": null,
                                  "conditionsComparison": "NOT_COMPARED",
                                  "observedIssues": []
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid_feedback_payload"))
                .andExpect(jsonPath("$.detail").value(
                        "Le retour contient une valeur inconnue ou un JSON invalide."));

        verifyNoInteractions(trackFeedbackService);
    }

    @Test
    void hidesMissingOrUnownedTracksBehindTheSameNotFoundResponse() throws Exception {
        given(trackFeedbackService.get(ACCOUNT_ID, TRACK_ID))
                .willThrow(new TrackNotFoundException());

        mockMvc.perform(get("/tracks/{trackId}/feedback", TRACK_ID)
                        .with(authentication(accountAuthentication())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("track_not_found"))
                .andExpect(jsonPath("$.detail").value(
                        "Cette trace n’existe pas ou n’est pas accessible."));
    }

    @Test
    void deletesFeedbackWithCsrfProtection() throws Exception {
        mockMvc.perform(deleteWithCsrf("/tracks/{trackId}/feedback", TRACK_ID)
                        .with(authentication(accountAuthentication()))
                )
                .andExpect(status().isNoContent());

        verify(trackFeedbackService).delete(ACCOUNT_ID, TRACK_ID);
    }

    private String validRequest() {
        return """
                {
                  "outcome": "COMPLETED_WITH_CHANGES",
                  "actualDurationMinutes": 405,
                  "perceivedEffort": 4,
                  "conditionsComparison": "WORSE_THAN_EXPECTED",
                  "observedIssues": ["WEATHER", "FATIGUE"]
                }
                """;
    }

    private MockHttpServletRequestBuilder putWithCsrf(
            String path,
            Object... uriVariables) throws Exception {
        Cookie csrfCookie = csrfCookie();
        return put(path, uriVariables)
                .cookie(csrfCookie)
                .header("X-XSRF-TOKEN", csrfCookie.getValue());
    }

    private MockHttpServletRequestBuilder deleteWithCsrf(
            String path,
            Object... uriVariables) throws Exception {
        Cookie csrfCookie = csrfCookie();
        return delete(path, uriVariables)
                .cookie(csrfCookie)
                .header("X-XSRF-TOKEN", csrfCookie.getValue());
    }

    private Cookie csrfCookie() throws Exception {
        Cookie csrfCookie = mockMvc.perform(get("/auth/csrf"))
                .andExpect(status().isNoContent())
                .andReturn()
                .getResponse()
                .getCookie("XSRF-TOKEN");

        if (csrfCookie == null) {
            throw new AssertionError("Missing XSRF-TOKEN test cookie.");
        }
        return csrfCookie;
    }

    private TrackFeedbackCommand validCommand() {
        return new TrackFeedbackCommand(
                FeedbackOutcome.COMPLETED_WITH_CHANGES,
                405,
                4,
                ConditionsComparison.WORSE_THAN_EXPECTED,
                Set.of(FeedbackIssue.WEATHER, FeedbackIssue.FATIGUE));
    }

    private TrackFeedbackView feedbackView() {
        return new TrackFeedbackView(
                TRACK_ID,
                FeedbackOutcome.COMPLETED_WITH_CHANGES,
                405,
                4,
                ConditionsComparison.WORSE_THAN_EXPECTED,
                Set.of(FeedbackIssue.WEATHER, FeedbackIssue.FATIGUE),
                CREATED_AT,
                UPDATED_AT);
    }

    private Authentication accountAuthentication() {
        AccountPrincipal principal = mock(AccountPrincipal.class);
        given(principal.id()).willReturn(ACCOUNT_ID);

        return org.springframework.security.authentication.UsernamePasswordAuthenticationToken
                .authenticated(
                        principal,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }
}
