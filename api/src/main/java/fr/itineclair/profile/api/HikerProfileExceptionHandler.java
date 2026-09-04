package fr.itineclair.profile.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import fr.itineclair.profile.InvalidHikerProfileException;

@RestControllerAdvice(assignableTypes = HikerProfileController.class)
public class HikerProfileExceptionHandler {

    @ExceptionHandler(InvalidHikerProfileException.class)
    public ResponseEntity<ProblemDetail> handleInvalidProfile(
            InvalidHikerProfileException exception) {
        return problem(
                HttpStatus.UNPROCESSABLE_CONTENT,
                "Profil pratiquant invalide",
                exception.getMessage(),
                "invalid_hiker_profile");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleUnreadableProfile() {
        return problem(
                HttpStatus.BAD_REQUEST,
                "Profil pratiquant illisible",
                "Le profil contient une valeur inconnue ou un JSON invalide.",
                "invalid_hiker_profile_payload");
    }

    private ResponseEntity<ProblemDetail> problem(
            HttpStatus status,
            String title,
            String detail,
            String code) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(status, detail);

        problem.setTitle(title);
        problem.setProperty("code", code);

        return ResponseEntity.status(status).body(problem);
    }
}
