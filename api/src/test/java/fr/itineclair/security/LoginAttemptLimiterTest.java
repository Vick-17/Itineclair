package fr.itineclair.security;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoginAttemptLimiterTest {

    private static final String EMAIL = "victor@example.test";
    private static final String CLIENT_IP = "203.0.113.10";

    private final AtomicLong nanoTime = new AtomicLong();

    @Test
    void blocksRepeatedAttemptsForTheSameAccountAndClient() {
        LoginAttemptLimiter limiter = limiter(
                20,
                2,
                50);

        limiter.beforeAuthentication(
                "  Victor@Example.Test  ",
                request(CLIENT_IP));
        limiter.beforeAuthentication(
                EMAIL,
                request(CLIENT_IP));

        assertThatThrownBy(() -> limiter.beforeAuthentication(
                EMAIL,
                request(CLIENT_IP)))
                .isInstanceOfSatisfying(
                        LoginRateLimitExceededException.class,
                        exception -> assertThat(
                                exception.retryAfterSeconds())
                                .isEqualTo(900L));
    }

    @Test
    void blocksOneClientAcrossDifferentAccounts() {
        LoginAttemptLimiter limiter = limiter(
                2,
                20,
                50);

        limiter.beforeAuthentication(
                "first@example.test",
                request(CLIENT_IP));
        limiter.beforeAuthentication(
                "second@example.test",
                request(CLIENT_IP));

        assertThatThrownBy(() -> limiter.beforeAuthentication(
                "third@example.test",
                request(CLIENT_IP)))
                .isInstanceOf(LoginRateLimitExceededException.class);
    }

    @Test
    void blocksDistributedAttemptsAgainstOneAccount() {
        LoginAttemptLimiter limiter = limiter(
                20,
                20,
                2);

        limiter.beforeAuthentication(EMAIL, request("203.0.113.10"));
        limiter.beforeAuthentication(EMAIL, request("203.0.113.11"));

        assertThatThrownBy(() -> limiter.beforeAuthentication(
                EMAIL,
                request("203.0.113.12")))
                .isInstanceOf(LoginRateLimitExceededException.class);
    }

    @Test
    void successfulAuthenticationReleasesIdentityReservations() {
        LoginAttemptLimiter limiter = limiter(
                20,
                1,
                1);

        LoginAttemptLimiter.Permit permit =
                limiter.beforeAuthentication(
                        EMAIL,
                        request(CLIENT_IP));

        limiter.authenticationSucceeded(permit);

        assertThat(limiter.beforeAuthentication(
                EMAIL,
                request(CLIENT_IP)))
                .isNotNull();
    }

    @Test
    void infrastructureFailureReleasesIdentityReservations() {
        LoginAttemptLimiter limiter = limiter(
                20,
                1,
                1);

        LoginAttemptLimiter.Permit permit =
                limiter.beforeAuthentication(
                        EMAIL,
                        request(CLIENT_IP));

        limiter.authenticationUnavailable(permit);

        assertThat(limiter.beforeAuthentication(
                EMAIL,
                request(CLIENT_IP)))
                .isNotNull();
    }

    @Test
    void attemptsExpireAtTheEndOfTheirWindow() {
        LoginAttemptLimiter limiter = limiter(
                20,
                1,
                50);

        limiter.beforeAuthentication(EMAIL, request(CLIENT_IP));

        assertThatThrownBy(() -> limiter.beforeAuthentication(
                EMAIL,
                request(CLIENT_IP)))
                .isInstanceOf(LoginRateLimitExceededException.class);

        nanoTime.addAndGet(Duration.ofMinutes(15).toNanos());

        assertThat(limiter.beforeAuthentication(
                EMAIL,
                request(CLIENT_IP)))
                .isNotNull();
    }

    @Test
    void errorNeverContainsTrackedIdentityOrAddress() {
        LoginAttemptLimiter limiter = limiter(
                20,
                1,
                50);

        limiter.beforeAuthentication(EMAIL, request(CLIENT_IP));

        assertThatThrownBy(() -> limiter.beforeAuthentication(
                EMAIL,
                request(CLIENT_IP)))
                .isInstanceOf(LoginRateLimitExceededException.class)
                .hasMessageNotContaining(EMAIL)
                .hasMessageNotContaining(CLIENT_IP);
    }

    @Test
    void concurrentAttemptsCannotExceedTheConfiguredLimit()
            throws Exception {
        int allowedAttempts = 5;
        int concurrentAttempts = 20;
        LoginAttemptLimiter limiter = limiter(
                concurrentAttempts,
                allowedAttempts,
                concurrentAttempts);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();

        try (ExecutorService executor =
                     Executors.newFixedThreadPool(concurrentAttempts)) {
            for (int index = 0; index < concurrentAttempts; index++) {
                results.add(executor.submit(() -> {
                    start.await();

                    try {
                        limiter.beforeAuthentication(
                                EMAIL,
                                request(CLIENT_IP));
                        return true;
                    } catch (LoginRateLimitExceededException exception) {
                        return false;
                    }
                }));
            }

            start.countDown();

            long accepted = 0;

            for (Future<Boolean> result : results) {
                if (result.get()) {
                    accepted++;
                }
            }

            assertThat(accepted).isEqualTo(allowedAttempts);
        }
    }

    private LoginAttemptLimiter limiter(
            int ipAttempts,
            int accountIpAttempts,
            int accountAttempts) {
        LoginProtectionProperties properties =
                new LoginProtectionProperties(
                        ipAttempts,
                        Duration.ofMinutes(1),
                        accountIpAttempts,
                        Duration.ofMinutes(15),
                        accountAttempts,
                        Duration.ofHours(1),
                        100);

        return new LoginAttemptLimiter(
                properties,
                nanoTime::get);
    }

    private MockHttpServletRequest request(String address) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(address);
        return request;
    }
}
