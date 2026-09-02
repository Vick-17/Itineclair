package fr.itineclair.privacy;

public class AccountActionRateLimitExceededException
        extends RuntimeException {

    private final long retryAfterSeconds;

    public AccountActionRateLimitExceededException(
            long retryAfterSeconds) {
        super("Sensitive account actions are temporarily rate limited.");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long retryAfterSeconds() {
        return retryAfterSeconds;
    }
}
