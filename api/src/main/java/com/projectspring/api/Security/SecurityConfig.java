package com.projectspring.api.Security;

import java.util.Arrays;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Classe de configuration des mécanismes de sécurité de l'application.
 */
@Configuration
public class SecurityConfig {

    String adminRole = "ROLE_ADMIN";

    /**
     * L'annotation @Bean est PRIMORDIALE et permet de spécifier que la méthode
     * renvoie une instance de classe qui
     * devra être gérée par "spring context"
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    /**
     * Méthode permettant de configurer la chaîne de filtres de sécurité de spring
     * 
     * Pour plus d'informations concernant la chaîne de filtres de Spring veuillez
     * vous référer à la
     * documentation suivante :
     * https://docs.spring.io/spring-security/reference/servlet/architecture.html#servlet-securityfilterchain
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, AuthenticationManager authenticationManager)
            throws Exception {

        // Le code suivant permet de modifier la chaîne de filtres afin de :
        // - autoriser toutes les requêtes sur le endpoint 'login'
        // - autoriser les requêtes sur le endpoint 'users' uniquement si l'utilisateur
        // a le "ROLE_ADMIN" et qu'il est authentifié
        // - ajouter les filtre d'authorisation et d'authentification

        http.csrf(csrf -> csrf.disable()) // désactivation de la vérification par défaut des attaques CSRF (pas grave vue qu'on va mettre en place un système de jetons)
                .cors(Customizer.withDefaults()) // Activer la configuration CORS définie dans CorsConfigurationSource
                .authorizeHttpRequests((authz) -> authz
                        .requestMatchers("/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .anyRequest().authenticated())
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // ajout du filtre pour la phase d'authentificaiton, utilisé uniquement lors de la phase de login
                .addFilter(new CustomAuthenticationFilter(authenticationManager))

                // filtre qui va se déclencher à chaque requete juste avavant le "CustomAuthentication"
                .addFilterBefore(new CustomAuthorizationFilter(), UsernamePasswordAuthenticationFilter.class) 
                .headers(headers -> headers.cacheControl(Customizer.withDefaults()));

        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("*")); // Origine autorisée
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS")); // Méthodes autorisées
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type")); // En-têtes autorisés
        configuration.setExposedHeaders(Arrays.asList("Authorization", "access_token", "refresh_token")); // En-têtes exposés
        configuration.setAllowCredentials(true); // Autorise les cookies si nécessaire

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration); // Applique à toutes les routes
        return source;
    }
}
