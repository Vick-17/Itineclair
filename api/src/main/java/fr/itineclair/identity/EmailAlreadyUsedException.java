package fr.itineclair.identity;

public class EmailAlreadyUsedException extends RuntimeException {

    public EmailAlreadyUsedException() {
        super("Email already in use");
    }

    public EmailAlreadyUsedException(Throwable cause) {
        super("Email already in use", cause);
    }
}
