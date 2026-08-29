package fr.itineclair.identity.api;

import fr.itineclair.identity.EmailAlreadyUsedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Comparator;
import java.util.Objects;

@RestControllerAdvice
public class IdentityExceptionHandler {

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