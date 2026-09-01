package fr.itineclair.sharing.api;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import fr.itineclair.sharing.InvalidTrackShareException;
import fr.itineclair.sharing.SharedReportNotFoundException;

@RestControllerAdvice
public class TrackShareExceptionHandler {

    @ExceptionHandler(SharedReportNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound() {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                "Ce partage n’existe plus ou n’est pas accessible.");
        problem.setTitle("Partage indisponible");
        problem.setProperty("code", "shared_report_not_found");

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .cacheControl(CacheControl.noStore())
                .header("Referrer-Policy", "no-referrer")
                .header("X-Robots-Tag", "noindex, nofollow")
                .body(problem);
    }

    @ExceptionHandler(InvalidTrackShareException.class)
    public ResponseEntity<ProblemDetail> handleInvalidShare(
            InvalidTrackShareException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_CONTENT,
                exception.getMessage());
        problem.setTitle("Partage invalide");
        problem.setProperty("code", "invalid_track_share");

        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_CONTENT)
                .body(problem);
    }
}
