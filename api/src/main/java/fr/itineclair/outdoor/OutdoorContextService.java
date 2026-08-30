package fr.itineclair.outdoor;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import fr.itineclair.track.TrackLocationService;
import fr.itineclair.track.TrackStartPoint;

@Service
public class OutdoorContextService {

    private static final int MINIMUM_DURATION_MINUTES = 30;
    private static final int MAXIMUM_DURATION_MINUTES = 1440;
    private static final Duration MAXIMUM_PAST_OFFSET = Duration.ofHours(1);
    private static final Duration MAXIMUM_FUTURE_OFFSET = Duration.ofDays(366);

    private final TrackOutdoorContextRepository contextRepository;
    private final TrackLocationService trackLocationService;
    private final DaylightCalculator daylightCalculator;
    private final WeatherForecastProvider weatherProvider;
    private final Clock clock;

    public OutdoorContextService(
            TrackOutdoorContextRepository contextRepository,
            TrackLocationService trackLocationService,
            DaylightCalculator daylightCalculator,
            WeatherForecastProvider weatherProvider,
            Clock clock) {
        this.contextRepository = contextRepository;
        this.trackLocationService = trackLocationService;
        this.daylightCalculator = daylightCalculator;
        this.weatherProvider = weatherProvider;
        this.clock = clock;
    }

    public Optional<OutdoorContextView> getContext(
            UUID ownerId,
            UUID trackId) {
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(trackId, "trackId");

        TrackStartPoint startPoint = trackLocationService
                .requireOwnedStartPoint(ownerId, trackId);

        return contextRepository
                .findById(trackId)
                .map(context -> toView(context, startPoint));
    }

    public OutdoorContextView saveContext(
            UUID ownerId,
            UUID trackId,
            OutdoorPlanCommand command) {
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(trackId, "trackId");
        Objects.requireNonNull(command, "command");

        TrackStartPoint startPoint = trackLocationService
                .requireOwnedStartPoint(ownerId, trackId);
        ResolvedPlan plan = resolveAndValidate(command);
        Instant now = clock.instant();

        TrackOutdoorContext context = contextRepository
                .findById(trackId)
                .orElseGet(() -> TrackOutdoorContext.create(
                        trackId,
                        plan.startAt(),
                        command.plannedDurationMinutes(),
                        plan.timeZone().getId(),
                        now));

        context.updatePlan(
                plan.startAt(),
                command.plannedDurationMinutes(),
                plan.timeZone().getId(),
                now);

        if (!command.shareStartPointWithWeatherProvider()) {
            context.clearWeather();
        } else {
            retrieveWeather(context, startPoint, plan, now);
        }

        TrackOutdoorContext saved = contextRepository.save(context);
        return toView(saved, startPoint);
    }

    private void retrieveWeather(
            TrackOutdoorContext context,
            TrackStartPoint startPoint,
            ResolvedPlan plan,
            Instant consentAt) {
        try {
            WeatherForecast forecast = weatherProvider.forecast(
                    new WeatherForecastRequest(
                            startPoint.latitude(),
                            startPoint.longitude(),
                            plan.startAt(),
                            plan.endAt()));
            context.recordWeather(forecast, consentAt);
        } catch (ForecastOutsideHorizonException exception) {
            context.recordWeatherFailure(
                    WeatherStatus.OUTSIDE_FORECAST_HORIZON,
                    weatherProvider.sourceName(),
                    weatherProvider.attributionUrl(),
                    consentAt,
                    clock.instant());
        } catch (WeatherProviderException exception) {
            context.recordWeatherFailure(
                    WeatherStatus.UNAVAILABLE,
                    weatherProvider.sourceName(),
                    weatherProvider.attributionUrl(),
                    consentAt,
                    clock.instant());
        }
    }

    private OutdoorContextView toView(
            TrackOutdoorContext context,
            TrackStartPoint startPoint) {
        ZoneId timeZone = ZoneId.of(context.timeZone());
        Instant plannedEndAt = context.plannedStartAt()
                .plusSeconds(context.plannedDurationMinutes() * 60L);
        DaylightWindow daylight = daylightCalculator.calculate(
                startPoint,
                context.plannedStartAt(),
                plannedEndAt,
                timeZone);

        return OutdoorContextView.from(
                context,
                daylight,
                weatherProvider);
    }

    private ResolvedPlan resolveAndValidate(
            OutdoorPlanCommand command) {
        if (command.plannedStartLocal() == null) {
            throw new InvalidOutdoorContextException(
                    "L’heure de départ est obligatoire.");
        }

        if (command.plannedDurationMinutes() < MINIMUM_DURATION_MINUTES
                || command.plannedDurationMinutes()
                > MAXIMUM_DURATION_MINUTES) {
            throw new InvalidOutdoorContextException(
                    "La durée prévue doit être comprise entre 30 minutes et 24 heures.");
        }

        ZoneId timeZone = parseTimeZone(command.timeZone());
        ZoneOffset offset = requireUnambiguousOffset(
                command.plannedStartLocal(),
                timeZone);
        Instant startAt = command.plannedStartLocal()
                .toInstant(offset);
        Instant now = clock.instant();

        if (startAt.isBefore(now.minus(MAXIMUM_PAST_OFFSET))) {
            throw new InvalidOutdoorContextException(
                    "L’heure de départ ne peut pas être dans le passé.");
        }

        if (startAt.isAfter(now.plus(MAXIMUM_FUTURE_OFFSET))) {
            throw new InvalidOutdoorContextException(
                    "La sortie ne peut pas être planifiée à plus d’un an.");
        }

        Instant endAt = startAt.plusSeconds(
                command.plannedDurationMinutes() * 60L);

        return new ResolvedPlan(startAt, endAt, timeZone);
    }

    private ZoneId parseTimeZone(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidOutdoorContextException(
                    "Le fuseau horaire est obligatoire.");
        }

        try {
            return ZoneId.of(value.strip());
        } catch (DateTimeException exception) {
            throw new InvalidOutdoorContextException(
                    "Le fuseau horaire n’est pas reconnu.");
        }
    }

    private ZoneOffset requireUnambiguousOffset(
            LocalDateTime localDateTime,
            ZoneId timeZone) {
        List<ZoneOffset> offsets = timeZone
                .getRules()
                .getValidOffsets(localDateTime);

        if (offsets.size() != 1) {
            throw new InvalidOutdoorContextException(
                    "Cette heure est inexistante ou ambiguë à cause du changement d’heure. Choisis une autre heure.");
        }

        return offsets.getFirst();
    }

    private record ResolvedPlan(
            Instant startAt,
            Instant endAt,
            ZoneId timeZone) {
    }
}
