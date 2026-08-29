package fr.itineclair.security;

import java.util.Locale;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

import fr.itineclair.identity.AccountPrincipal;
import fr.itineclair.identity.InvalidCredentialsException;

@Service
public class SessionAuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final SessionAuthenticationStrategy sessionAuthenticationStrategy;
    private final SecurityContextRepository securityContextRepository;
    private final SecurityContextHolderStrategy securityContextHolderStrategy;

    public SessionAuthenticationService(
            AuthenticationManager authenticationManager,
            SessionAuthenticationStrategy sessionAuthenticationStrategy,
            SecurityContextRepository securityContextRepository,
            SecurityContextHolderStrategy securityContextHolderStrategy) {
        this.authenticationManager = authenticationManager;
        this.sessionAuthenticationStrategy = sessionAuthenticationStrategy;
        this.securityContextRepository = securityContextRepository;
        this.securityContextHolderStrategy = securityContextHolderStrategy;
    }

    public AccountPrincipal authenticate(
            String email,
            String rawPassword,
            HttpServletRequest request,
            HttpServletResponse response) {
        Authentication authentication =
                authenticateCredentials(email, rawPassword);

        if (!(authentication.getPrincipal()
                instanceof AccountPrincipal principal)) {
            throw new AuthenticationServiceException(
                    "Unexpected authenticated principal type.");
        }

        sessionAuthenticationStrategy.onAuthentication(
                authentication,
                request,
                response);

        SecurityContext context = securityContextHolderStrategy
                .createEmptyContext();

        context.setAuthentication(authentication);
        securityContextHolderStrategy.setContext(context);

        securityContextRepository.saveContext(
                context,
                request,
                response);

        return principal;
    }

    private Authentication authenticateCredentials(
            String email,
            String rawPassword) {
        String normalizedEmail = email
                .strip()
                .toLowerCase(Locale.ROOT);

        var authenticationRequest =
                UsernamePasswordAuthenticationToken
                        .unauthenticated(normalizedEmail, rawPassword);

        try {
            return authenticationManager.authenticate(authenticationRequest);
        } catch (AuthenticationServiceException exception) {
            throw exception;
        } catch (AuthenticationException exception) {
            throw new InvalidCredentialsException();
        }
    }
}
