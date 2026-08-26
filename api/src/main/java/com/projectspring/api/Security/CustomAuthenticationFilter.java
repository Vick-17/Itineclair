package com.projectspring.api.Security;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.util.MimeTypeUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Filtre permettant de gérer la phase d'authentification
 */
public class CustomAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    private static final String BAD_CREDENTIAL_MESSAGE = "Echec de l'autentification pour l'utilisateur : %s";

    private AuthenticationManager authenticationManager;

    private Logger logger = LoggerFactory.getLogger(CustomAuthenticationFilter.class);

    /**
     * Constructeur prenant automatique en paramètre le bean de "SecurityConfig"
     * 
     * @param authenticationManager Bean géré par la classe "SecurityConfig"
     */
    public CustomAuthenticationFilter(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    /**
     * Cette méthode est appelée lors de la phase de login.
     * Elle prend le nom d'utilisateur et le mot de passe du corps de la requête
     * (utilisation d'un ObjetMapper pour récupérer le contenu du JSon)
     * 
     * Json devant être envoyé par le client :
     * {
     * "name": "<nom-utilisateur>",
     * "password: "<password>
     * }
     */
    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException {

        String username = null;
        String password = null;
        try {
            // Utilisation d'un ObjectMapper pour désérialiser le json envoyé via une
            // requête POST
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, String> map = objectMapper.readValue(request.getInputStream(), Map.class);
            username = map.get("name");
            password = map.get("password");

            logger.debug("Authentification pour l'utilisateur: {}", username);

            // création d'un objet d'une classe héritant de "Authentication"
            // ici nous utilisons "UsernamePasswordAuthenticationToken" car la vérification
            // se faire
            // en utilisant le nom d'utilisateur et son mot de passe
            // ce qui est magique c'est que c'est le "PasswordEncoder" définit dans
            // "HostelApplication" qui va être utilisé
            Authentication userAuthentication = new UsernamePasswordAuthenticationToken(username, password);

            // tentative d'authentification
            return authenticationManager.authenticate(userAuthentication);
        } catch (AuthenticationException e) {
            logger.error(String.format(BAD_CREDENTIAL_MESSAGE, username), e);
            throw e;
        } catch (Exception e) {
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            Map<String, String> error = new HashMap<>();
            error.put("errorMessage", e.getMessage());
            response.setContentType(MimeTypeUtils.APPLICATION_JSON_VALUE);

            // écriture du message d'erreur dans la réponse
            ObjectMapper om = new ObjectMapper();
            try {
                om.writeValue(response.getOutputStream(), error);
            } catch (Exception e2) {
                logger.debug("Erreur lors de l'écriture de la requête réponse à l'authentification utilisateur.");
            }

            throw new RuntimeException(String
                    .format("Erreur lors de la tentive d'authentification avec le nom d'utilisateur %s", username), e);
        }
    }

    /**
     * Méthode appelée automatiquement "attemptAuthentication" est un succès.
     */
    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain,
            Authentication authentication) throws IOException, ServletException {

        UserDetails user = (UserDetails) authentication.getPrincipal();
        // création du JWT token
        // 1 - on récupère les roles de l'utilisateur
        Collection<? extends GrantedAuthority> authorities = user.getAuthorities();

        // 2 - on récupère une liste de chaînes de caractères indiquant les rôles de
        // l'utilisateur
        List<String> stringAuthorities = authorities.stream().map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        // 3 - on crée le JWT avec le nom de l'utilisateur, l'URL du serveur fournissant
        // le jeton et une représentaiton en chaîne de caractères
        // des rôles
        String accessToken = JwtUtil.createAccessToken(user.getUsername(), request.getRequestURL().toString(),
                stringAuthorities);

        logger.info(String.format("Création d'un token pour l'utilisateur : {}. Token : ", user.getUsername(),
                accessToken));
        String refreshToken = JwtUtil.createRefreshToken(user.getUsername());
        // modification de l'en-tête de la requête de retour pour ajouter les JWT
        response.addHeader("access_token", accessToken);
        response.addHeader("refresh_token", refreshToken);
    }

    /**
     * Méthode appelée lorsque "attemptAuthentication" lève une exception de type
     * "AuthenticationException "
     */
    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException failed) throws IOException, ServletException {

        // status de la réponse : 401 = non autorisé !
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        ObjectMapper mapper = new ObjectMapper();
        // ajout d'un message d'erreur au corps de la réponse
        Map<String, String> error = new HashMap<>();
        error.put("errorMessage", "Mauvaises informations de connexion");
        response.setContentType(MimeTypeUtils.APPLICATION_JSON_VALUE);

        // écriture de l'erreur dans le corps de la réponse
        mapper.writeValue(response.getOutputStream(), error);
    }
}
