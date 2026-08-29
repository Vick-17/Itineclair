package fr.itineclair.identity.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import fr.itineclair.identity.AccountPrincipal;
import fr.itineclair.identity.AccountRegistrationService;
import fr.itineclair.identity.RegisteredAccount;
import fr.itineclair.security.LoginAttemptLimiter;
import fr.itineclair.security.SessionAuthenticationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AccountRegistrationService accountRegistrationService;
    private final SessionAuthenticationService sessionAuthenticationService;
    private final LoginAttemptLimiter loginAttemptLimiter;

    public AuthController(
            AccountRegistrationService accountRegistrationService,
            SessionAuthenticationService sessionAuthenticationService,
            LoginAttemptLimiter loginAttemptLimiter) {
        this.accountRegistrationService = accountRegistrationService;
        this.sessionAuthenticationService = sessionAuthenticationService;
        this.loginAttemptLimiter = loginAttemptLimiter;
    }

    @GetMapping("/csrf")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void csrf(CsrfToken csrfToken) {
        csrfToken.getToken();
    }

    @PostMapping("/register")
    public ResponseEntity<RegisteredAccountResponse> register(
            @Valid @RequestBody RegisterAccountRequest request) {
        RegisteredAccount account = accountRegistrationService.register(
                request.email(), request.password());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(RegisteredAccountResponse.from(account));
    }

    @PostMapping("/login")
    public AuthenticatedAccountResponse login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        LoginAttemptLimiter.Permit permit =
                loginAttemptLimiter.beforeAuthentication(
                        request.email(),
                        httpRequest);

        try {
            AccountPrincipal principal =
                    sessionAuthenticationService.authenticate(
                            request.email(),
                            request.password(),
                            httpRequest,
                            httpResponse);

            loginAttemptLimiter.authenticationSucceeded(permit);
            return AuthenticatedAccountResponse.from(principal);
        } catch (AuthenticationServiceException exception) {
            loginAttemptLimiter.authenticationUnavailable(permit);
            throw exception;
        }
    }

    @GetMapping("/me")
    public AuthenticatedAccountResponse currentAccount(
            @AuthenticationPrincipal AccountPrincipal principal) {
        return AuthenticatedAccountResponse.from(principal);
    }
}
