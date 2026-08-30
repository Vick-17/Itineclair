package fr.itineclair.track.api;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import jakarta.servlet.http.Cookie;

import fr.itineclair.identity.AccountPrincipal;
import fr.itineclair.identity.AccountRegistrationService;
import fr.itineclair.identity.api.AuthController;
import fr.itineclair.security.LoginAttemptLimiter;
import fr.itineclair.security.SecurityConfiguration;
import fr.itineclair.security.SessionAuthenticationService;
import fr.itineclair.track.InvalidGpxException;
import fr.itineclair.track.TrackFacts;
import fr.itineclair.track.TrackImportService;
import fr.itineclair.track.TrackNotFoundException;
import fr.itineclair.track.TrackSummary;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        TrackController.class,
        AuthController.class
})
@Import({
        SecurityConfiguration.class,
        TrackExceptionHandler.class
})
class TrackControllerTest {

    private static final UUID ACCOUNT_ID =
            UUID.fromString("936dd470-a45c-46fa-a0bd-94a76e4b836a");

    private static final UUID TRACK_ID =
            UUID.fromString("ce7b58c7-d6f4-4a26-8bad-68265bdb7bbf");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TrackImportService trackImportService;

    @MockitoBean
    private AccountRegistrationService accountRegistrationService;

    @MockitoBean
    private SessionAuthenticationService sessionAuthenticationService;

    @MockitoBean
    private LoginAttemptLimiter loginAttemptLimiter;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void importsGpxForAuthenticatedAccount() throws Exception {
        TrackSummary imported = summary();
        given(trackImportService.importGpx(
                eq(ACCOUNT_ID),
                any()))
                .willReturn(imported);

        mockMvc.perform(multipartWithCsrf("/tracks")
                        .file(gpxFile())
                        .with(authentication(accountAuthentication())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id")
                        .value(TRACK_ID.toString()))
                .andExpect(jsonPath("$.name")
                        .value("Tour du lac"))
                .andExpect(jsonPath("$.segmentCount")
                        .value(2))
                .andExpect(jsonPath("$.pointCount")
                        .value(120))
                .andExpect(jsonPath("$.elevationPointCount")
                        .value(120))
                .andExpect(jsonPath("$.elevationComplete")
                        .value(true))
                .andExpect(jsonPath("$.facts.distanceMeters")
                        .value(12_450.5))
                .andExpect(jsonPath("$.facts.elevationGainMeters")
                        .value(850.0))
                .andExpect(jsonPath("$.facts.minimumElevationMeters")
                        .value(1_020.0))
                .andExpect(jsonPath("$.facts.maximumUphillGradePercent")
                        .value(18.4))
                .andExpect(jsonPath("$.facts.gradeMinimumRunMeters")
                        .value(25));
    }

    @Test
    void listsOnlyCurrentAccountTracks() throws Exception {
        given(trackImportService.listTracks(ACCOUNT_ID))
                .willReturn(List.of(summary()));

        mockMvc.perform(get("/tracks")
                        .with(authentication(accountAuthentication())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id")
                        .value(TRACK_ID.toString()))
                .andExpect(jsonPath("$[0].sourceFilename")
                        .value("tour-du-lac.gpx"));
    }

    @Test
    void returnsAnOwnedTrackReport() throws Exception {
        given(trackImportService.getTrack(
                ACCOUNT_ID,
                TRACK_ID))
                .willReturn(summary());

        mockMvc.perform(get("/tracks/{trackId}", TRACK_ID)
                        .with(authentication(accountAuthentication())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id")
                        .value(TRACK_ID.toString()))
                .andExpect(jsonPath("$.facts.distanceMeters")
                        .value(12_450.5));
    }

    @Test
    void hidesMissingOrUnownedTracksBehindSameNotFoundResponse()
            throws Exception {
        given(trackImportService.getTrack(
                ACCOUNT_ID,
                TRACK_ID))
                .willThrow(new TrackNotFoundException());

        mockMvc.perform(get("/tracks/{trackId}", TRACK_ID)
                        .with(authentication(accountAuthentication())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value("track_not_found"))
                .andExpect(jsonPath("$.detail")
                        .value("Cette trace n’existe pas ou n’est pas accessible."));
    }

    @Test
    void rejectsImportWithoutAuthentication() throws Exception {
        mockMvc.perform(multipartWithCsrf("/tracks")
                        .file(gpxFile()))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(trackImportService);
    }

    @Test
    void rejectsImportWithoutCsrfToken() throws Exception {
        mockMvc.perform(multipart("/tracks")
                        .file(gpxFile())
                        .with(authentication(accountAuthentication())))
                .andExpect(status().isForbidden());

        verifyNoInteractions(trackImportService);
    }

    @Test
    void returnsSafeProblemForInvalidGpx() throws Exception {
        given(trackImportService.importGpx(
                eq(ACCOUNT_ID),
                any()))
                .willThrow(new InvalidGpxException(
                        "La trace GPX doit contenir au moins deux points."));

        mockMvc.perform(multipartWithCsrf("/tracks")
                        .file(gpxFile())
                        .with(authentication(accountAuthentication())))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code")
                        .value("invalid_gpx"))
                .andExpect(jsonPath("$.detail")
                        .value("La trace GPX doit contenir au moins deux points."));
    }

    private MockMultipartHttpServletRequestBuilder multipartWithCsrf(
            String path) throws Exception {
        Cookie csrfCookie = mockMvc.perform(get("/auth/csrf"))
                .andExpect(status().isNoContent())
                .andReturn()
                .getResponse()
                .getCookie("XSRF-TOKEN");

        if (csrfCookie == null) {
            throw new AssertionError("Missing XSRF-TOKEN test cookie.");
        }

        return multipart(path)
                .cookie(csrfCookie)
                .header("X-XSRF-TOKEN", csrfCookie.getValue());
    }

    private MockMultipartFile gpxFile() {
        return new MockMultipartFile(
                "file",
                "tour-du-lac.gpx",
                "application/gpx+xml",
                "<gpx/>".getBytes(StandardCharsets.UTF_8));
    }

    private TrackSummary summary() {
        return new TrackSummary(
                TRACK_ID,
                "Tour du lac",
                "tour-du-lac.gpx",
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
                Instant.parse("2026-08-29T12:00:00Z"));
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
