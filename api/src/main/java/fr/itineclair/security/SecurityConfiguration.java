package fr.itineclair.security;

import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password4j.Argon2Password4jPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

import com.password4j.Argon2Function;
import com.password4j.types.Argon2;

import jakarta.servlet.DispatcherType;

@Configuration(proxyBeanMethods = false)
public class SecurityConfiguration {

    private static final String PASSWORD_ENCODING_ID = "argon2id";

    private static final int ARGON2_MEMORY_KIB = 19 * 1024;
    private static final int ARGON2_ITERATIONS = 2;
    private static final int ARGON2_PARALLELISM = 1;
    private static final int ARGON2_HASH_LENGTH = 32;

    @Bean
    public PasswordEncoder passwordEncoder() {
        Argon2Function argon2Function = Argon2Function.getInstance(
                ARGON2_MEMORY_KIB,
                ARGON2_ITERATIONS,
                ARGON2_PARALLELISM,
                ARGON2_HASH_LENGTH,
                Argon2.ID);

        PasswordEncoder argon2id =
                new Argon2Password4jPasswordEncoder(argon2Function);

        return new DelegatingPasswordEncoder(
                PASSWORD_ENCODING_ID,
                Map.of(PASSWORD_ENCODING_ID, argon2id));
    }

    @Bean
    public SecurityFilterChain apiSecurity(HttpSecurity http)
            throws Exception {

        http
                .authorizeHttpRequests(authorize -> authorize
                        .dispatcherTypeMatchers(DispatcherType.ERROR)
                        .permitAll()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/auth/csrf",
                                "/actuator/health",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html")
                        .permitAll()

                        .requestMatchers(
                                HttpMethod.POST,
                                "/auth/register")
                        .permitAll()

                        .anyRequest()
                        .authenticated()
                )
                .csrf(csrf -> csrf.spa())
                .requestCache(requestCache -> requestCache.disable())
                .formLogin(formLogin -> formLogin.disable())
                .httpBasic(httpBasic -> httpBasic.disable())
                .logout(logout -> logout.disable())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(
                                new HttpStatusEntryPoint(
                                        HttpStatus.UNAUTHORIZED)));

        return http.build();
    }
}
