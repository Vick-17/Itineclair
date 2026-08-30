package fr.itineclair.outdoor.api;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
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

import fr.itineclair.identity.AccountPrincipal;
import fr.itineclair.identity.AccountRegistrationService;
import fr.itineclair.identity.api.AuthController;
import fr.itineclair.identity.api.IdentityExceptionHandler;
import fr.itineclair.outdoor.DaylightCondition;
import fr.itineclair.outdoor.DaylightWindow;
import fr.itineclair.outdoor.InvalidOutdoorContextException;
import fr.itineclair.outdoor.OutdoorContextService;
import fr.itineclair.outdoor.OutdoorContextView;
import fr.itineclair.outdoor.OutdoorPlanCommand;
import fr.itineclair.outdoor.WeatherStatus;
import fr.itineclair.outdoor.WeatherSummary;
import fr.itineclair.security.LoginAttemptLimiter;
import fr.itineclair.security.SecurityConfiguration;
import fr.itineclair.security.SessionAuthenticationService;
import fr.itineclair.track.api.TrackExceptionHandler;
import jakarta.servlet.http.Cookie;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        OutdoorContextController.class,
        AuthController.class
})
@Import({
        SecurityConfiguration.class,
        OutdoorContextExceptionHandler.class,
        IdentityExceptionHandler.class,
        TrackExceptionHandler.class
})
class OutdoorContextControllerTest {

    private static final UUID ACCOUNT_ID =
            UUID.fromString("936dd470-a45c-46fa-a0bd-94a76e4b836a");
    private static final UUID TRACK_ID =
            UUID.fromString("ce7b58c7-d6f4-4a26-8bad-68265bdb7bbf");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OutdoorContextService outdoorContextService;

    @MockitoBean
    private AccountRegistrationService accountRegistrationService;

    @MockitoBean
    private SessionAuthenticationService sessionAuthenticationService;

    @MockitoBean
    private LoginAttemptLimiter loginAttemptLimiter;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void returnsAnExplicitUnplannedResponse() throws Exception {
        given(outdoorContextService.getContext(
                ACCOUNT_ID,
                TRACK_ID))
                .willReturn(Optional.empty());

        mockMvc.perform(get(
                        "/tracks/{trackId}/outdoor-context",
                        TRACK_ID)
                        .with(authentication(accountAuthentication())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planned").value(false))
                .andExpect(jsonPath("$.plannedStartAt").doesNotExist());
    }

    @Test
    void savesAPlanWithExplicitWeatherConsent() throws Exception {
        OutdoorContextView view = contextView();
        OutdoorPlanCommand expectedCommand = new OutdoorPlanCommand(
                LocalDateTime.parse("2026-08-31T08:00:00"),
                360,
                "Europe/Paris",
                true);
        given(outdoorContextService.saveContext(
                ACCOUNT_ID,
                TRACK_ID,
                expectedCommand))
                .willReturn(view);

        mockMvc.perform(putWithCsrf(
                        "/tracks/{trackId}/outdoor-context",
                        TRACK_ID)
                        .with(authentication(accountAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "plannedStartLocal": "2026-08-31T08:00:00",
                                  "plannedDurationMinutes": 360,
                                  "timeZone": "Europe/Paris",
                                  "shareStartPointWithWeatherProvider": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planned").value(true))
                .andExpect(jsonPath("$.plannedStartAt")
                        .value("2026-08-31T06:00:00Z"))
                .andExpect(jsonPath("$.daylight.expectedDarknessMinutes")
                        .value(0))
                .andExpect(jsonPath("$.weather.status")
                        .value("AVAILABLE"))
                .andExpect(jsonPath("$.weather.maximumWindGustKilometersPerHour")
                        .value(58.0));

        verify(outdoorContextService).saveContext(
                eq(ACCOUNT_ID),
                eq(TRACK_ID),
                eq(expectedCommand));
    }

    @Test
    void rejectsMutationWithoutCsrfToken() throws Exception {
        mockMvc.perform(put(
                        "/tracks/{trackId}/outdoor-context",
                        TRACK_ID)
                        .with(authentication(accountAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(outdoorContextService);
    }

    @Test
    void requiresAnExplicitWeatherSharingChoice() throws Exception {
        mockMvc.perform(putWithCsrf(
                        "/tracks/{trackId}/outdoor-context",
                        TRACK_ID)
                        .with(authentication(accountAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "plannedStartLocal": "2026-08-31T08:00:00",
                                  "plannedDurationMinutes": 360,
                                  "timeZone": "Europe/Paris"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("validation_failed"))
                .andExpect(jsonPath("$.violations[0].field")
                        .value("shareStartPointWithWeatherProvider"));

        verifyNoInteractions(outdoorContextService);
    }

    @Test
    void returnsAControlledSemanticValidationProblem() throws Exception {
        given(outdoorContextService.saveContext(
                eq(ACCOUNT_ID),
                eq(TRACK_ID),
                eq(new OutdoorPlanCommand(
                        LocalDateTime.parse("2026-08-31T08:00:00"),
                        360,
                        "Europe/Paris",
                        true))))
                .willThrow(new InvalidOutdoorContextException(
                        "Cette heure est ambiguë."));

        mockMvc.perform(putWithCsrf(
                        "/tracks/{trackId}/outdoor-context",
                        TRACK_ID)
                        .with(authentication(accountAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code")
                        .value("invalid_outdoor_context"))
                .andExpect(jsonPath("$.detail")
                        .value("Cette heure est ambiguë."));
    }

    private MockHttpServletRequestBuilder putWithCsrf(
            String path,
            Object... uriVariables) throws Exception {
        Cookie csrfCookie = mockMvc.perform(get("/auth/csrf"))
                .andExpect(status().isNoContent())
                .andReturn()
                .getResponse()
                .getCookie("XSRF-TOKEN");

        if (csrfCookie == null) {
            throw new AssertionError("Missing XSRF-TOKEN test cookie.");
        }

        return put(path, uriVariables)
                .cookie(csrfCookie)
                .header("X-XSRF-TOKEN", csrfCookie.getValue());
    }

    private String validRequest() {
        return """
                {
                  "plannedStartLocal": "2026-08-31T08:00:00",
                  "plannedDurationMinutes": 360,
                  "timeZone": "Europe/Paris",
                  "shareStartPointWithWeatherProvider": true
                }
                """;
    }

    private OutdoorContextView contextView() {
        return new OutdoorContextView(
                LocalDateTime.parse("2026-08-31T08:00:00"),
                Instant.parse("2026-08-31T06:00:00Z"),
                Instant.parse("2026-08-31T12:00:00Z"),
                360,
                "Europe/Paris",
                Instant.parse("2026-08-30T10:00:00Z"),
                new DaylightWindow(
                        Instant.parse("2026-08-31T04:51:00Z"),
                        Instant.parse("2026-08-31T18:16:00Z"),
                        Instant.parse("2026-08-31T04:19:00Z"),
                        Instant.parse("2026-08-31T18:48:00Z"),
                        360,
                        0,
                        DaylightCondition.NORMAL),
                new WeatherSummary(
                        WeatherStatus.AVAILABLE,
                        "Open-Meteo",
                        "https://open-meteo.com/",
                        Instant.parse("2026-08-30T10:00:00Z"),
                        Instant.parse("2026-08-31T06:00:00Z"),
                        Instant.parse("2026-08-31T13:00:00Z"),
                        5.0,
                        12.0,
                        2.0,
                        10.0,
                        70,
                        3.2,
                        0.0,
                        32.0,
                        58.0,
                        1_210.0));
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
