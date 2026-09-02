package fr.itineclair.privacy;

public class AccountDataUnavailableException extends RuntimeException {

    public AccountDataUnavailableException() {
        super("The account data is unavailable.");
    }
}
