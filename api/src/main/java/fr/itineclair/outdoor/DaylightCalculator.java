package fr.itineclair.outdoor;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.shredzone.commons.suncalc.SunTimes;
import org.springframework.stereotype.Component;

import fr.itineclair.track.TrackStartPoint;

@Component
public class DaylightCalculator {

    public DaylightWindow calculate(
            TrackStartPoint startPoint,
            Instant plannedStartAt,
            Instant plannedEndAt,
            ZoneId timeZone) {
        LocalDate departureDate = plannedStartAt
                .atZone(timeZone)
                .toLocalDate();

        SunTimes visual = sunTimes(
                startPoint,
                departureDate,
                timeZone,
                SunTimes.Twilight.VISUAL);
        SunTimes civil = sunTimes(
                startPoint,
                departureDate,
                timeZone,
                SunTimes.Twilight.CIVIL);

        long totalSeconds = Duration.between(
                plannedStartAt,
                plannedEndAt).toSeconds();
        long daylightSeconds = daylightOverlapSeconds(
                startPoint,
                plannedStartAt,
                plannedEndAt,
                timeZone);
        long darknessSeconds = Math.max(
                0L,
                totalSeconds - daylightSeconds);

        return new DaylightWindow(
                toInstant(visual.getRise()),
                toInstant(visual.getSet()),
                toInstant(civil.getRise()),
                toInstant(civil.getSet()),
                daylightSeconds / 60L,
                ceilMinutes(darknessSeconds),
                condition(civil));
    }

    private long daylightOverlapSeconds(
            TrackStartPoint startPoint,
            Instant plannedStartAt,
            Instant plannedEndAt,
            ZoneId timeZone) {
        LocalDate currentDate = plannedStartAt
                .atZone(timeZone)
                .toLocalDate();
        LocalDate finalDate = plannedEndAt
                .atZone(timeZone)
                .toLocalDate();
        long overlapSeconds = 0L;

        while (!currentDate.isAfter(finalDate)) {
            SunTimes civil = sunTimes(
                    startPoint,
                    currentDate,
                    timeZone,
                    SunTimes.Twilight.CIVIL);

            if (civil.isAlwaysUp()) {
                Instant dayStart = currentDate
                        .atStartOfDay(timeZone)
                        .toInstant();
                Instant dayEnd = currentDate
                        .plusDays(1)
                        .atStartOfDay(timeZone)
                        .toInstant();
                overlapSeconds += overlapSeconds(
                        plannedStartAt,
                        plannedEndAt,
                        dayStart,
                        dayEnd);
            } else if (!civil.isAlwaysDown()
                    && civil.getRise() != null
                    && civil.getSet() != null) {
                overlapSeconds += overlapSeconds(
                        plannedStartAt,
                        plannedEndAt,
                        civil.getRise().toInstant(),
                        civil.getSet().toInstant());
            }

            currentDate = currentDate.plusDays(1);
        }

        return overlapSeconds;
    }

    private long overlapSeconds(
            Instant outingStart,
            Instant outingEnd,
            Instant daylightStart,
            Instant daylightEnd) {
        Instant overlapStart = outingStart.isAfter(daylightStart)
                ? outingStart
                : daylightStart;
        Instant overlapEnd = outingEnd.isBefore(daylightEnd)
                ? outingEnd
                : daylightEnd;

        if (!overlapEnd.isAfter(overlapStart)) {
            return 0L;
        }

        return Duration.between(overlapStart, overlapEnd).toSeconds();
    }

    private SunTimes sunTimes(
            TrackStartPoint startPoint,
            LocalDate date,
            ZoneId timeZone,
            SunTimes.Twilight twilight) {
        ZonedDateTime localMidnight = date.atStartOfDay(timeZone);
        double elevation = startPoint.elevationMeters() == null
                ? 0.0
                : Math.max(0.0, startPoint.elevationMeters());

        return SunTimes.compute()
                .on(localMidnight)
                .at(startPoint.latitude(), startPoint.longitude())
                .elevation(elevation)
                .twilight(twilight)
                .oneDay()
                .execute();
    }

    private DaylightCondition condition(SunTimes civil) {
        if (civil.isAlwaysUp()) {
            return DaylightCondition.SUN_ALWAYS_UP;
        }

        if (civil.isAlwaysDown()) {
            return DaylightCondition.SUN_ALWAYS_DOWN;
        }

        return DaylightCondition.NORMAL;
    }

    private Instant toInstant(ZonedDateTime dateTime) {
        return dateTime == null ? null : dateTime.toInstant();
    }

    private long ceilMinutes(long seconds) {
        return seconds == 0L ? 0L : (seconds + 59L) / 60L;
    }
}
