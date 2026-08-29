package fr.itineclair.identity.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import fr.itineclair.identity.AccountRegistrationService;
import fr.itineclair.identity.RegisteredAccount;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AccountRegistrationService accountRegistrationService;

    public AuthController(AccountRegistrationService accountRegistrationService) {
        this.accountRegistrationService = accountRegistrationService;
    }

    @GetMapping("/csrf")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void csrf(CsrfToken csrfToken) {
        csrfToken.getToken();
    }

    @PostMapping("/register")
    public ResponseEntity<RegisteredAccountResponse> register(@Valid @RequestBody RegisterAccountRequest request) {
        RegisteredAccount account = accountRegistrationService.register(request.email(), request.password());
        return ResponseEntity.status(HttpStatus.CREATED).body(RegisteredAccountResponse.from(account));
    }
}
