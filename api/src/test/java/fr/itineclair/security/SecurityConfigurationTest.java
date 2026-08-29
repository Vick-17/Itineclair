package fr.itineclair.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

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
}
