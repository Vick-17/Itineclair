package fr.itineclair.privacy;

public class AccountConfirmationMismatchException extends RuntimeException {

    public AccountConfirmationMismatchException() {
        super("The account confirmation does not match.");
    }
}
