package fr.itineclair.security;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class SecurityConfigurationTest {

    private static final String PASSWORD = "une phrase de passe suffisamment longue";

    @Test
    void hashesPasswordsWithArgon2idAndRandomSalt() {
        PasswordEncoder encoder = new SecurityConfiguration().passwordEncoder();

        String firstHash = encoder.encode(PASSWORD);
        String secondHash = encoder.encode(PASSWORD);

        assertThat(firstHash)
                .startsWith("{argon2id}$argon2id$");

        assertThat(firstHash)
                .isNotEqualTo(secondHash);

        assertThat(encoder.matches(PASSWORD, firstHash))
                .isTrue();

        assertThat(encoder.matches("mauvais mot de passe", firstHash))
                .isFalse();
    }

    @Test
    void authenticatesPasswordThroughDaoProvider() {
        SecurityConfiguration configuration = new SecurityConfiguration();
        PasswordEncoder encoder = configuration.passwordEncoder();
        UserDetailsService userDetailsService = mock(UserDetailsService.class);
        given(userDetailsService.loadUserByUsername("victor@example.test"))
                .willReturn(User.builder()
                        .username("victor@example.test")
                        .password(encoder.encode(PASSWORD))
                        .roles("USER")
                        .build());

        AuthenticationManager authenticationManager =
                configuration.authenticationManager(userDetailsService, encoder);
        Authentication result = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(
                        "victor@example.test", PASSWORD));

        assertThat(result.isAuthenticated()).isTrue();
        assertThat(result.getName()).isEqualTo("victor@example.test");
        assertThat(result.getCredentials()).isNull();
    }

    @Test
    void createsReadableCsrfCookieWithExplicitBrowserProtections() {
        CsrfTokenRepository repository =
                new SecurityConfiguration().csrfTokenRepository(true);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        CsrfToken token = repository.generateToken(request);
        repository.saveToken(token, request, response);

        Cookie cookie = response.getCookie("XSRF-TOKEN");
        assertThat(cookie).isNotNull();
        assertThat(cookie.getValue()).isEqualTo(token.getToken());
        assertThat(cookie.getPath()).isEqualTo("/");
        assertThat(cookie.isHttpOnly()).isFalse();
        assertThat(cookie.getSecure()).isTrue();
        assertThat(cookie.getAttribute("SameSite")).isEqualTo("Lax");
    }

    @Test
    void rotatesExistingSessionIdAndClearsCsrfTokenOnLogin() {
        SecurityConfiguration configuration = new SecurityConfiguration();
        CsrfTokenRepository csrfTokenRepository =
                configuration.csrfTokenRepository(false);
        CsrfTokenRequestHandler csrfTokenRequestHandler =
                configuration.csrfTokenRequestHandler();
        SessionAuthenticationStrategy strategy =
                configuration.sessionAuthenticationStrategy(
                        csrfTokenRepository,
                        csrfTokenRequestHandler,
                        mock(ApplicationEventPublisher.class));

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpSession session = (MockHttpSession) request.getSession();
        String originalSessionId = session.getId();
        request.setRequestedSessionId(originalSessionId);
        request.setRequestedSessionIdValid(true);
        request.setCookies(new Cookie("XSRF-TOKEN", "token-before-login"));

        strategy.onAuthentication(mock(Authentication.class), request, response);

        assertThat(session.getId()).isNotEqualTo(originalSessionId);
        assertThat(response.getCookie("XSRF-TOKEN").getMaxAge()).isZero();
    }
}
