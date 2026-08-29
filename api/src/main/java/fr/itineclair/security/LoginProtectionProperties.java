package fr.itineclair.security;

import java.time.Duration;
import java.util.Objects;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "itineclair.security.login-protection")
public record LoginProtectionProperties(
        int ipAttempts,
        Duration ipWindow,
        int accountIpAttempts,
        Duration accountIpWindow,
        int accountAttempts,
        Duration accountWindow,
        long maximumKeysPerScope) {

    public LoginProtectionProperties {
        requirePositive(ipAttempts, "ip-attempts");
        requirePositive(accountIpAttempts, "account-ip-attempts");
        requirePositive(accountAttempts, "account-attempts");
        requirePositive(maximumKeysPerScope, "maximum-keys-per-scope");
        requirePositive(ipWindow, "ip-window");
        requirePositive(accountIpWindow, "account-ip-window");
        requirePositive(accountWindow, "account-window");
    }

    private static void requirePositive(long value, String property) {
        if (value <= 0) {
            throw new IllegalArgumentException(
                    property + " must be greater than zero.");
        }
    }

    private static void requirePositive(
            Duration value,
            String property) {
        Objects.requireNonNull(value, property + " is required.");

        if (value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(
                    property + " must be greater than zero.");
        }
    }
}
