package fr.itineclair.analysis.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import fr.itineclair.analysis.AnalysisCategory;
import fr.itineclair.analysis.AnalysisChecklistItem;
import fr.itineclair.analysis.AnalysisEvidence;
import fr.itineclair.analysis.AnalysisFinding;
import fr.itineclair.analysis.AnalysisSeverity;
import fr.itineclair.analysis.AnalysisSourceSnapshot;
import fr.itineclair.analysis.ChecklistStatus;
import fr.itineclair.analysis.EvidenceComparison;
import fr.itineclair.analysis.RuleReviewStatus;
import fr.itineclair.analysis.TrackAnalysis;
import fr.itineclair.analysis.TrackAnalysisService;
import fr.itineclair.identity.AccountPrincipal;
import fr.itineclair.identity.AccountRegistrationService;
import fr.itineclair.identity.api.AuthController;
import fr.itineclair.identity.api.IdentityExceptionHandler;
import fr.itineclair.security.LoginAttemptLimiter;
import fr.itineclair.security.SecurityConfiguration;
import fr.itineclair.security.SessionAuthenticationService;
import fr.itineclair.track.api.TrackExceptionHandler;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        TrackAnalysisController.class,
        AuthController.class
})
@Import({
        SecurityConfiguration.class,
        IdentityExceptionHandler.class,
        TrackExceptionHandler.class
})
class TrackAnalysisControllerTest {

    private static final UUID ACCOUNT_ID =
            UUID.fromString("936dd470-a45c-46fa-a0bd-94a76e4b836a");
    private static final UUID TRACK_ID =
            UUID.fromString("ce7b58c7-d6f4-4a26-8bad-68265bdb7bbf");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TrackAnalysisService trackAnalysisService;

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
        mockMvc.perform(get(
                        "/tracks/{trackId}/analysis",
                        TRACK_ID))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(trackAnalysisService);
    }

    @Test
    void returnsVersionedFindingsEvidenceAndChecklist() throws Exception {
        given(trackAnalysisService.analyze(ACCOUNT_ID, TRACK_ID))
                .willReturn(analysis());

        mockMvc.perform(get(
                        "/tracks/{trackId}/analysis",
                        TRACK_ID)
                        .with(authentication(accountAuthentication())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ruleSetVersion").value(1))
                .andExpect(jsonPath("$.reviewStatus").value(
                        "PROTOTYPE_AWAITING_EXPERT_REVIEW"))
                .andExpect(jsonPath("$.sourceSnapshot.factsVersion")
                        .value(1))
                .andExpect(jsonPath("$.findings[0].code")
                        .value("STRONG_GUSTS_AT_START"))
                .andExpect(jsonPath("$.findings[0].category")
                        .value("WEATHER"))
                .andExpect(jsonPath("$.findings[0].severity")
                        .value("STRONG_CAUTION"))
                .andExpect(jsonPath("$.findings[0].evidence[0].metric")
                        .value("maximumWindGustKilometersPerHour"))
                .andExpect(jsonPath("$.findings[0].evidence[0].observedValue")
                        .value(75.0))
                .andExpect(jsonPath("$.findings[0].evidence[0].thresholdValue")
                        .value(70.0))
                .andExpect(jsonPath("$.checklist[0].status")
                        .value("PARTIAL"))
                .andExpect(jsonPath("$.limitations[0]").isNotEmpty());
    }

    private TrackAnalysis analysis() {
        Instant generatedAt = Instant.parse("2026-08-30T12:00:00Z");
        return new TrackAnalysis(
                1,
                RuleReviewStatus.PROTOTYPE_AWAITING_EXPERT_REVIEW,
                generatedAt,
                new AnalysisSourceSnapshot(
                        1,
                        generatedAt.minusSeconds(300),
                        generatedAt.minusSeconds(600)),
                List.of(new AnalysisFinding(
                        "STRONG_GUSTS_AT_START",
                        AnalysisCategory.WEATHER,
                        AnalysisSeverity.STRONG_CAUTION,
                        "Rafales marquées prévues au point de départ",
                        "Prévision ponctuelle.",
                        "Consulter le bulletin montagne.",
                        List.of(new AnalysisEvidence(
                                "maximumWindGustKilometersPerHour",
                                "Rafale maximale",
                                75.0,
                                "km/h",
                                EvidenceComparison.GREATER_OR_EQUAL,
                                70.0)))),
                List.of(new AnalysisChecklistItem(
                        "POINT_WEATHER",
                        ChecklistStatus.PARTIAL,
                        "Prévision météo",
                        "Prévision ponctuelle disponible.")),
                List.of("Limite connue."));
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
