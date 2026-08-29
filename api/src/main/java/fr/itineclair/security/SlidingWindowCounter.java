package fr.itineclair.security;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;

final class SlidingWindowCounter {

    private final Deque<Attempt> attempts = new ArrayDeque<>();
    private long nextReservationId;

    synchronized Reservation reserve(
            long nowNanos,
            int limit,
            Duration window) {
        long windowNanos = window.toNanos();
        removeExpiredAttempts(nowNanos, windowNanos);

        if (attempts.size() >= limit) {
            long oldestAttemptAge = nowNanos - attempts.getFirst().recordedAtNanos();
            long retryAfterNanos = Math.max(1, windowNanos - oldestAttemptAge);

            return Reservation.rejected(
                    Duration.ofNanos(retryAfterNanos));
        }

        long reservationId = ++nextReservationId;
        attempts.addLast(new Attempt(reservationId, nowNanos));

        return Reservation.allowed(reservationId);
    }

    synchronized void cancel(long reservationId) {
        attempts.removeIf(attempt ->
                attempt.reservationId() == reservationId);
    }

    private void removeExpiredAttempts(
            long nowNanos,
            long windowNanos) {
        while (!attempts.isEmpty()
                && nowNanos - attempts.getFirst().recordedAtNanos()
                >= windowNanos) {
            attempts.removeFirst();
        }
    }

    private record Attempt(
            long reservationId,
            long recordedAtNanos) {
    }

    record Reservation(
            boolean allowed,
            long reservationId,
            Duration retryAfter) {

        private static Reservation allowed(long reservationId) {
            return new Reservation(
                    true,
                    reservationId,
                    Duration.ZERO);
        }

        private static Reservation rejected(Duration retryAfter) {
            return new Reservation(
                    false,
                    0,
                    retryAfter);
        }
    }
}
