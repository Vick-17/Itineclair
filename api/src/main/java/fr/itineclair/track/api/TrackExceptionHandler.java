package fr.itineclair.track.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import fr.itineclair.track.GpxFileTooLargeException;
import fr.itineclair.track.InvalidGpxException;
import fr.itineclair.track.UnsupportedGpxFileException;

@RestControllerAdvice
public class TrackExceptionHandler {

    @ExceptionHandler(InvalidGpxException.class)
    public ResponseEntity<ProblemDetail> handleInvalidGpx(
            InvalidGpxException exception) {
        return problem(
                HttpStatus.UNPROCESSABLE_CONTENT,
                "Fichier GPX invalide",
                exception.getMessage(),
                "invalid_gpx");
    }

    @ExceptionHandler(UnsupportedGpxFileException.class)
    public ResponseEntity<ProblemDetail> handleUnsupportedGpx(
            UnsupportedGpxFileException exception) {
        return problem(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Format de fichier non pris en charge",
                exception.getMessage(),
                "unsupported_gpx_file");
    }

    @ExceptionHandler({
            GpxFileTooLargeException.class,
            MaxUploadSizeExceededException.class
    })
    public ResponseEntity<ProblemDetail> handleGpxTooLarge() {
        return problem(
                HttpStatus.PAYLOAD_TOO_LARGE,
                "Fichier GPX trop volumineux",
                "Le fichier GPX dépasse la taille maximale autorisée de 10 Mo.",
                "gpx_file_too_large");
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ProblemDetail> handleMissingFile() {
        return problem(
                HttpStatus.BAD_REQUEST,
                "Fichier GPX manquant",
                "Sélectionne un fichier GPX avant de lancer l’import.",
                "gpx_file_required");
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ProblemDetail> handleInvalidMultipart() {
        return problem(
                HttpStatus.BAD_REQUEST,
                "Import illisible",
                "La requête d’import GPX n’a pas pu être lue.",
                "invalid_gpx_upload");
    }

    private ResponseEntity<ProblemDetail> problem(
            HttpStatus status,
            String title,
            String detail,
            String code) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                status,
                detail);
        problem.setTitle(title);
        problem.setProperty("code", code);

        return ResponseEntity
                .status(status)
                .body(problem);
    }
}
