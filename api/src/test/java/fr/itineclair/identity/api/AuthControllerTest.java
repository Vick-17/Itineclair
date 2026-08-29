package fr.itineclair.identity.api;

import fr.itineclair.identity.AccountRegistrationService;
import fr.itineclair.identity.EmailAlreadyUsedException;
import fr.itineclair.identity.RegisteredAccount;
import fr.itineclair.security.SecurityConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Instant;
import java.util.UUID;

import jakarta.servlet.http.Cookie;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
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
}
