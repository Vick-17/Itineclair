package fr.itineclair.security;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.function.LongSupplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class LoginAttemptLimiter {

    private static final String UNKNOWN_CLIENT = "unknown-client";

    private final LoginProtectionProperties properties;
    private final LongSupplier nanoTime;
    private final Cache<String, SlidingWindowCounter> ipAttempts;
    private final Cache<String, SlidingWindowCounter> accountIpAttempts;
    private final Cache<String, SlidingWindowCounter> accountAttempts;

    @Autowired
    public LoginAttemptLimiter(LoginProtectionProperties properties) {
        this(properties, System::nanoTime);
    }

    LoginAttemptLimiter(
            LoginProtectionProperties properties,
            LongSupplier nanoTime) {
        this.properties = properties;
        this.nanoTime = nanoTime;
        this.ipAttempts = newCounterCache(properties.ipWindow());
        this.accountIpAttempts = newCounterCache(
                properties.accountIpWindow());
        this.accountAttempts = newCounterCache(
                properties.accountWindow());
    }

    public Permit beforeAuthentication(
            String email,
            HttpServletRequest request) {
        Objects.requireNonNull(request, "request is required.");

        String accountKey = digest(
                "account",
                normalizeEmail(email));
        String clientKey = digest(
                "client",
                clientAddress(request));
        String accountIpKey = accountKey + ':' + clientKey;
        long nowNanos = nanoTime.getAsLong();

        reserveOrReject(
                ipAttempts,
                clientKey,
                properties.ipAttempts(),
                properties.ipWindow(),
                nowNanos);

        CounterReservation accountIpReservation = reserveOrReject(
                accountIpAttempts,
                accountIpKey,
                properties.accountIpAttempts(),
                properties.accountIpWindow(),
                nowNanos);

        CounterReservation accountReservation;

        try {
            accountReservation = reserveOrReject(
                    accountAttempts,
                    accountKey,
                    properties.accountAttempts(),
                    properties.accountWindow(),
                    nowNanos);
        } catch (LoginRateLimitExceededException exception) {
            accountIpReservation.cancel();
            throw exception;
        }

        return new Permit(
                accountIpReservation,
                accountReservation);
    }

    public void authenticationSucceeded(Permit permit) {
        releaseIdentityReservations(permit);
    }

    public void authenticationUnavailable(Permit permit) {
        releaseIdentityReservations(permit);
    }

    private void releaseIdentityReservations(Permit permit) {
        Objects.requireNonNull(permit, "permit is required.");
        permit.accountIpReservation.cancel();
        permit.accountReservation.cancel();
    }

    private CounterReservation reserveOrReject(
            Cache<String, SlidingWindowCounter> cache,
            String key,
            int limit,
            Duration window,
            long nowNanos) {
        SlidingWindowCounter counter = cache.get(
                key,
                ignored -> new SlidingWindowCounter());
        SlidingWindowCounter.Reservation reservation = counter.reserve(
                nowNanos,
                limit,
                window);

        if (!reservation.allowed()) {
            throw new LoginRateLimitExceededException(
                    reservation.retryAfter());
        }

        return new CounterReservation(
                counter,
                reservation.reservationId());
    }

    private Cache<String, SlidingWindowCounter> newCounterCache(
            Duration expiry) {
        return Caffeine.newBuilder()
                .maximumSize(properties.maximumKeysPerScope())
                .expireAfterAccess(expiry)
                .build();
    }

    private String normalizeEmail(String email) {
        return Objects.requireNonNull(email, "email is required.")
                .strip()
                .toLowerCase(Locale.ROOT);
    }

    private String clientAddress(HttpServletRequest request) {
        String address = request.getRemoteAddr();

        return address == null || address.isBlank()
                ? UNKNOWN_CLIENT
                : address;
    }

    private String digest(String namespace, String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(namespace.getBytes(UTF_8));
            digest.update((byte) 0);
            return HexFormat.of().formatHex(
                    digest.digest(value.getBytes(UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 is unavailable.",
                    exception);
        }
    }

    public static final class Permit {

        private final CounterReservation accountIpReservation;
        private final CounterReservation accountReservation;

        private Permit(
                CounterReservation accountIpReservation,
                CounterReservation accountReservation) {
            this.accountIpReservation = accountIpReservation;
            this.accountReservation = accountReservation;
        }
    }

    private record CounterReservation(
            SlidingWindowCounter counter,
            long reservationId) {

        private void cancel() {
            counter.cancel(reservationId);
        }
    }
}
