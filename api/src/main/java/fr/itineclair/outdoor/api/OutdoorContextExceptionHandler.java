package fr.itineclair.outdoor.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import fr.itineclair.outdoor.InvalidOutdoorContextException;

@RestControllerAdvice
public class OutdoorContextExceptionHandler {

    @ExceptionHandler(InvalidOutdoorContextException.class)
    public ResponseEntity<ProblemDetail> handleInvalidContext(
            InvalidOutdoorContextException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_CONTENT,
                exception.getMessage());
        problem.setTitle("Contexte de sortie invalide");
        problem.setProperty("code", "invalid_outdoor_context");

        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_CONTENT)
                .body(problem);
    }
}
