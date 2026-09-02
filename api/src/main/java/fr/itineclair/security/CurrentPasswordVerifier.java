package fr.itineclair.security;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

import fr.itineclair.identity.AccountPrincipal;

@Service
public class CurrentPasswordVerifier {

    private final AuthenticationManager authenticationManager;

    public CurrentPasswordVerifier(
            AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    public boolean matches(
            AccountPrincipal currentPrincipal,
            String rawPassword) {
        var authenticationRequest =
                UsernamePasswordAuthenticationToken.unauthenticated(
                        currentPrincipal.email(),
                        rawPassword);

        try {
            Authentication authentication = authenticationManager
                    .authenticate(authenticationRequest);

            return authentication.getPrincipal()
                    instanceof AccountPrincipal verifiedPrincipal
                    && currentPrincipal.id().equals(verifiedPrincipal.id());
        } catch (AuthenticationServiceException exception) {
            throw exception;
        } catch (AuthenticationException exception) {
            return false;
        }
    }
}
