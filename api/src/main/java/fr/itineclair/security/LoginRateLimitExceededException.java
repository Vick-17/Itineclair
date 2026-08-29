package fr.itineclair.security;

import java.time.Duration;
import java.util.Objects;

public class LoginRateLimitExceededException extends RuntimeException {

    private final long retryAfterSeconds;

    public LoginRateLimitExceededException(Duration retryAfter) {
        super("Too many authentication attempts.");

        Duration requiredRetryAfter = Objects.requireNonNull(
                retryAfter,
                "retryAfter is required.");
        long roundedSeconds = requiredRetryAfter.getSeconds();

        if (requiredRetryAfter.getNano() > 0) {
            roundedSeconds = Math.addExact(roundedSeconds, 1);
        }

        this.retryAfterSeconds = Math.max(1, roundedSeconds);
    }

    public long retryAfterSeconds() {
        return retryAfterSeconds;
    }
}
