package fr.itineclair.privacy.api;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import fr.itineclair.privacy.AccountActionRateLimitExceededException;
import fr.itineclair.privacy.AccountConfirmationMismatchException;
import fr.itineclair.privacy.AccountDataUnavailableException;
import fr.itineclair.privacy.InvalidCurrentPasswordException;

@RestControllerAdvice
public class PrivacyExceptionHandler {

    @ExceptionHandler(InvalidCurrentPasswordException.class)
    public ResponseEntity<ProblemDetail> handleInvalidCurrentPassword() {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                "Le mot de passe courant est incorrect.");
        problem.setTitle("Vérification impossible");
        problem.setProperty("code", "invalid_current_password");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem);
    }

    @ExceptionHandler(AccountConfirmationMismatchException.class)
    public ResponseEntity<ProblemDetail> handleConfirmationMismatch() {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "L’adresse e-mail de confirmation ne correspond pas au compte.");
        problem.setTitle("Confirmation incorrecte");
        problem.setProperty("code", "account_confirmation_mismatch");
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(AccountDataUnavailableException.class)
    public ResponseEntity<ProblemDetail> handleAccountDataUnavailable() {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                "Les données du compte ne sont plus disponibles.");
        problem.setTitle("Compte indisponible");
        problem.setProperty("code", "account_data_unavailable");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    @ExceptionHandler(AccountActionRateLimitExceededException.class)
    public ResponseEntity<ProblemDetail> handleAccountActionRateLimit(
            AccountActionRateLimitExceededException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.TOO_MANY_REQUESTS,
                "Trop de tentatives. Réessaie plus tard.");
        problem.setTitle("Action temporairement limitée");
        problem.setProperty("code", "account_action_rate_limited");

        return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .header(
                        HttpHeaders.RETRY_AFTER,
                        Long.toString(exception.retryAfterSeconds()))
                .body(problem);
    }
}
