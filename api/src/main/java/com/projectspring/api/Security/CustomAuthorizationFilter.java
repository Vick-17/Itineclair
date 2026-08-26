package com.projectspring.api.Security;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Classe gérant la phase de validation des droits de l'utilisateur (ou
 * "Authorization").
 * 
 * Son objectif est de lire le JWT fourni par l'utilisateur et valider son bon
 * fonctionnement.
 */
public class CustomAuthorizationFilter extends OncePerRequestFilter {

    private Logger logger = LoggerFactory.getLogger(CustomAuthorizationFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = null;
        // si le requête s'effectue sur les endpoints "/login" ou "/refreshToken"
        // alors on appelle le filtre de sécurité suivant : CustomAuthenticationFiler
        if (request.getServletPath().equals("/login") || request.getServletPath().equals("/refreshToken")) {

            // SimpleGrantedAuthority authorities = new SimpleGrantedAuthority("ROLE_USER");
            // SecurityContextHolder.getContext().setAuthentication(new
            // UsernamePasswordAuthenticationToken("test", null,
            // Arrays.asList(authorities)));
            filterChain.doFilter(request, response);
        } else {
            // si le client fait un appel à autre chose que "/login" ou "/refreshToken"
            // alors on se retrouve ici
            // on récupère le contenu du header
            String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                try {
                    // le fameux token contient "Bearer", on s'en débarasse
                    token = authorizationHeader.substring("Bearer ".length());
                    // on parse le token
                    UsernamePasswordAuthenticationToken authenticationToken = JwtUtil.parseToken(token);
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                    // on enchaîne avec le prochain filtre
                    filterChain.doFilter(request, response);
                } catch (Exception e) {
                    logger.error(String.format("Erreur avec le JWT suivant : %s", token), e);
                    response.setStatus(HttpStatus.FORBIDDEN.value());

                    // construction du message qui est renvoyé à l'utilisateur
                    Map<String, String> error = new HashMap<>();
                    error.put("errorMessage", e.getMessage());
                    response.setContentType(MimeTypeUtils.APPLICATION_JSON_VALUE);

                    new ObjectMapper().writeValue(response.getOutputStream(), error);
                }
            } else {
                // on enchaîne avec le prochain filtre
                filterChain.doFilter(request, response);
            }
        }
    }
}
