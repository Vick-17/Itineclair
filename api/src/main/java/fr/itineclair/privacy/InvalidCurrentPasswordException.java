package fr.itineclair.privacy;

public class InvalidCurrentPasswordException extends RuntimeException {

    public InvalidCurrentPasswordException() {
        super("The current password is invalid.");
    }
}
