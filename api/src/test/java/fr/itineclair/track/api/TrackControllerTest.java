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

import fr.itineclair.identity.AccountPrincipal;
import fr.itineclair.security.SecurityConfiguration;
import fr.itineclair.track.InvalidGpxException;
import fr.itineclair.track.TrackImportService;
import fr.itineclair.track.TrackSummary;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.security.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TrackController.class)
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
    private UserDetailsService userDetailsService;

    @Test
    void importsGpxForAuthenticatedAccount() throws Exception {
        TrackSummary imported = summary();
        given(trackImportService.importGpx(
                eq(ACCOUNT_ID),
                any()))
                .willReturn(imported);

        mockMvc.perform(multipart("/tracks")
                        .file(gpxFile())
                        .with(csrf().asHeader())
                        .with(authentication(authentication())))
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
                        .value(true));
    }

    @Test
    void listsOnlyCurrentAccountTracks() throws Exception {
        given(trackImportService.listTracks(ACCOUNT_ID))
                .willReturn(List.of(summary()));

        mockMvc.perform(get("/tracks")
                        .with(authentication(authentication())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id")
                        .value(TRACK_ID.toString()))
                .andExpect(jsonPath("$[0].sourceFilename")
                        .value("tour-du-lac.gpx"));
    }

    @Test
    void rejectsImportWithoutAuthentication() throws Exception {
        mockMvc.perform(multipart("/tracks")
                        .file(gpxFile())
                        .with(csrf().asHeader()))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(trackImportService);
    }

    @Test
    void rejectsImportWithoutCsrfToken() throws Exception {
        mockMvc.perform(multipart("/tracks")
                        .file(gpxFile())
                        .with(authentication(authentication())))
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

        mockMvc.perform(multipart("/tracks")
                        .file(gpxFile())
                        .with(csrf().asHeader())
                        .with(authentication(authentication())))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code")
                        .value("invalid_gpx"))
                .andExpect(jsonPath("$.detail")
                        .value("La trace GPX doit contenir au moins deux points."));
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
                Instant.parse("2026-08-29T12:00:00Z"));
    }

    private Authentication authentication() {
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
