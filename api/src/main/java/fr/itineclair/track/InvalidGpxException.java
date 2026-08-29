package fr.itineclair.track;

public class InvalidGpxException extends RuntimeException {

    public InvalidGpxException(String message) {
        super(message);
    }

    public InvalidGpxException(String message, Throwable cause) {
        super(message, cause);
    }
}
