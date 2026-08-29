package fr.itineclair.identity.api;

import fr.itineclair.identity.EmailAlreadyUsedException;
import fr.itineclair.identity.InvalidCredentialsException;
import fr.itineclair.security.LoginRateLimitExceededException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.security.authentication.AuthenticationServiceException;

import java.util.Comparator;
import java.util.Objects;

@RestControllerAdvice
public class IdentityExceptionHandler {

    @ExceptionHandler(LoginRateLimitExceededException.class)
    public ResponseEntity<ProblemDetail> handleLoginRateLimit(
            LoginRateLimitExceededException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.TOO_MANY_REQUESTS,
                "Trop de tentatives de connexion. Réessaie plus tard.");
        problem.setTitle("Connexion temporairement limitée");
        problem.setProperty("code", "login_rate_limited");

        return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .header(
                        HttpHeaders.RETRY_AFTER,
                        Long.toString(exception.retryAfterSeconds()))
                .body(problem);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ProblemDetail> handleInvalidCredentials() {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                "L’adresse e-mail ou le mot de passe est incorrect.");
        problem.setTitle("Connexion impossible");
        problem.setProperty("code", "invalid_credentials");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem);
    }

    @ExceptionHandler(AuthenticationServiceException.class)
    public ResponseEntity<ProblemDetail> handleAuthenticationUnavailable() {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE,
                "La connexion est temporairement indisponible.");
        problem.setTitle("Service de connexion indisponible");
        problem.setProperty("code", "authentication_unavailable");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(problem);
    }

    @ExceptionHandler(EmailAlreadyUsedException.class)
    public ResponseEntity<ProblemDetail> handleEmailAlreadyUsed() {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "Un compte utilise déjà cette adresse e-mail.");

        problem.setTitle("Adresse e-mail indisponible");
        problem.setProperty("code", "email_already_used");

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(problem);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(
            MethodArgumentNotValidException exception) {
        var violations = exception
                .getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new FieldViolation(
                        error.getField(),
                        Objects.requireNonNullElse(
                                error.getDefaultMessage(),
                                "Valeur invalide.")))
                .sorted(Comparator.comparing(FieldViolation::field))
                .toList();

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "La requête contient une ou plusieurs valeurs invalides.");

        problem.setTitle("Requête invalide");
        problem.setProperty("code", "validation_failed");
        problem.setProperty("violations", violations);

        return ResponseEntity
                .badRequest()
                .body(problem);
    }

    public record FieldViolation(
            String field,
            String message) {
    }
}
