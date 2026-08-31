package fr.itineclair.feedback.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import fr.itineclair.feedback.InvalidTrackFeedbackException;

@RestControllerAdvice(assignableTypes = TrackFeedbackController.class)
public class TrackFeedbackExceptionHandler {

    @ExceptionHandler(InvalidTrackFeedbackException.class)
    public ResponseEntity<ProblemDetail> handleInvalidFeedback(
            InvalidTrackFeedbackException exception) {
        return problem(HttpStatus.UNPROCESSABLE_CONTENT,
                "Retour post-sortie invalide", exception.getMessage(),
                "invalid_track_feedback");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleUnreadableFeedback() {
        return problem(HttpStatus.BAD_REQUEST,
                "Retour post-sortie illisible",
                "Le retour contient une valeur inconnue ou un JSON invalide.",
                "invalid_feedback_payload");
    }

    private ResponseEntity<ProblemDetail> problem(
            HttpStatus status, String title, String detail, String code) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setProperty("code", code);
        return ResponseEntity.status(status).body(problem);
    }
}
