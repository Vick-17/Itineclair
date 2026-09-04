package fr.itineclair.profile.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request
        .SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
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
import fr.itineclair.profile.ExperienceLevel;
import fr.itineclair.profile.HikerProfileCommand;
import fr.itineclair.profile.HikerProfileService;
import fr.itineclair.profile.HikerProfileView;
import fr.itineclair.profile.InvalidHikerProfileException;
import fr.itineclair.security.LoginAttemptLimiter;
import fr.itineclair.security.SecurityConfiguration;
import fr.itineclair.security.SessionAuthenticationService;
import jakarta.servlet.http.Cookie;

@WebMvcTest(controllers = {
        HikerProfileController.class,
        AuthController.class
})
@Import({
        SecurityConfiguration.class,
        HikerProfileExceptionHandler.class,
        IdentityExceptionHandler.class
})
class HikerProfileControllerTest {

    private static final UUID ACCOUNT_ID =
            UUID.fromString("936dd470-a45c-46fa-a0bd-94a76e4b836a");
    private static final Instant CREATED_AT =
            Instant.parse("2026-09-02T10:00:00Z");
    private static final Instant UPDATED_AT =
            Instant.parse("2026-09-02T11:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HikerProfileService hikerProfileService;

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
        mockMvc.perform(get("/profile"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(hikerProfileService);
    }

    @Test
    void returnsAnExplicitUnconfiguredProfile() throws Exception {
        given(hikerProfileService.get(ACCOUNT_ID))
                .willReturn(Optional.empty());

        mockMvc.perform(get("/profile")
                        .with(authentication(accountAuthentication())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configured").value(false))
                .andExpect(jsonPath("$.experienceLevel").doesNotExist())
                .andExpect(jsonPath("$.usualDurationMinutes").doesNotExist())
                .andExpect(jsonPath("$.usualDistanceMeters").doesNotExist())
                .andExpect(jsonPath("$.usualElevationGainMeters")
                        .doesNotExist());
    }

    @Test
    void savesOnlyTheAuthenticatedAccountsProfileWithCsrfProtection()
            throws Exception {
        HikerProfileCommand command = validCommand();
        given(hikerProfileService.save(ACCOUNT_ID, command))
                .willReturn(profileView());

        mockMvc.perform(withCsrf(put("/profile"))
                        .with(authentication(accountAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configured").value(true))
                .andExpect(jsonPath("$.experienceLevel")
                        .value("REGULAR"))
                .andExpect(jsonPath("$.usualDurationMinutes")
                        .value(360))
                .andExpect(jsonPath("$.usualDistanceMeters")
                        .value(14_000))
                .andExpect(jsonPath("$.usualElevationGainMeters")
                        .value(900))
                .andExpect(jsonPath("$.updatedAt")
                        .value("2026-09-02T11:00:00Z"))
                .andExpect(content().string(
                        not(containsString(ACCOUNT_ID.toString()))));

        verify(hikerProfileService).save(ACCOUNT_ID, command);
    }

    @Test
    void rejectsSaveWithoutCsrfToken() throws Exception {
        mockMvc.perform(put("/profile")
                        .with(authentication(accountAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(hikerProfileService);
    }

    @Test
    void validatesRangesBeforeCallingTheService() throws Exception {
        mockMvc.perform(withCsrf(put("/profile"))
                        .with(authentication(accountAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "experienceLevel": "REGULAR",
                                  "usualDurationMinutes": 14,
                                  "usualDistanceMeters": 14000,
                                  "usualElevationGainMeters": 900
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("validation_failed"))
                .andExpect(jsonPath("$.violations[0].field")
                        .value("usualDurationMinutes"));

        verifyNoInteractions(hikerProfileService);
    }

    @Test
    void rejectsAnUnknownLevelWithoutLeakingThePayload()
            throws Exception {
        mockMvc.perform(withCsrf(put("/profile"))
                        .with(authentication(accountAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "experienceLevel": "CERTIFIED_EXPERT",
                                  "usualDurationMinutes": null,
                                  "usualDistanceMeters": null,
                                  "usualElevationGainMeters": null
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("invalid_hiker_profile_payload"))
                .andExpect(content().string(
                        not(containsString("CERTIFIED_EXPERT"))));

        verifyNoInteractions(hikerProfileService);
    }

    @Test
    void returnsAControlledSemanticValidationProblem()
            throws Exception {
        HikerProfileCommand command = validCommand();
        given(hikerProfileService.save(ACCOUNT_ID, command))
                .willThrow(new InvalidHikerProfileException(
                        "Profil incohérent."));

        mockMvc.perform(withCsrf(put("/profile"))
                        .with(authentication(accountAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code")
                        .value("invalid_hiker_profile"));
    }

    @Test
    void deletesOnlyTheAuthenticatedAccountsProfileWithCsrfProtection()
            throws Exception {
        mockMvc.perform(withCsrf(delete("/profile"))
                        .with(authentication(accountAuthentication())))
                .andExpect(status().isNoContent());

        verify(hikerProfileService).delete(ACCOUNT_ID);
    }

    @Test
    void rejectsDeleteWithoutCsrfToken() throws Exception {
        mockMvc.perform(delete("/profile")
                        .with(authentication(accountAuthentication())))
                .andExpect(status().isForbidden());

        verifyNoInteractions(hikerProfileService);
    }

    private String validRequest() {
        return """
                {
                  "experienceLevel": "REGULAR",
                  "usualDurationMinutes": 360,
                  "usualDistanceMeters": 14000,
                  "usualElevationGainMeters": 900
                }
                """;
    }

    private HikerProfileCommand validCommand() {
        return new HikerProfileCommand(
                ExperienceLevel.REGULAR,
                360,
                14_000,
                900);
    }

    private HikerProfileView profileView() {
        return new HikerProfileView(
                ACCOUNT_ID,
                ExperienceLevel.REGULAR,
                360,
                14_000,
                900,
                CREATED_AT,
                UPDATED_AT);
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
