package fr.itineclair.outdoor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import fr.itineclair.track.TrackLocationService;
import fr.itineclair.track.TrackNotFoundException;
import fr.itineclair.track.TrackStartPoint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class OutdoorContextServiceTest {

    private static final UUID OWNER_ID =
            UUID.fromString("936dd470-a45c-46fa-a0bd-94a76e4b836a");
    private static final UUID TRACK_ID =
            UUID.fromString("ce7b58c7-d6f4-4a26-8bad-68265bdb7bbf");
    private static final Instant NOW =
            Instant.parse("2026-08-30T10:00:00Z");
    private static final TrackStartPoint START_POINT =
            new TrackStartPoint(45.8326, 6.8652, 1_035.0);

    @Mock
    private TrackOutdoorContextRepository contextRepository;

    @Mock
    private TrackLocationService trackLocationService;

    @Mock
    private DaylightCalculator daylightCalculator;

    @Mock
    private WeatherForecastProvider weatherProvider;

    private OutdoorContextService service;

    @BeforeEach
    void setUp() {
        service = new OutdoorContextService(
                contextRepository,
                trackLocationService,
                daylightCalculator,
                weatherProvider,
                Clock.fixed(NOW, ZoneOffset.UTC));

        lenient().when(weatherProvider.sourceName())
                .thenReturn("Open-Meteo");
        lenient().when(weatherProvider.attributionUrl())
                .thenReturn("https://open-meteo.com/");
    }

    @Test
    void savesLocalDaylightWithoutSharingLocation() {
        prepareOwnedTrackAndPersistence();

        OutdoorContextView result = service.saveContext(
                OWNER_ID,
                TRACK_ID,
                command(false));

        assertThat(result.plannedStartAt())
                .isEqualTo("2026-08-31T06:00:00Z");
        assertThat(result.timeZone()).isEqualTo("Europe/Paris");
        assertThat(result.weather().status())
                .isEqualTo(WeatherStatus.NOT_REQUESTED);
        verify(weatherProvider, never()).forecast(any());
    }

    @Test
    void fetchesWeatherOnlyAfterExplicitConsent() {
        prepareOwnedTrackAndPersistence();
        WeatherForecast forecast = forecast();
        given(weatherProvider.forecast(any())).willReturn(forecast);

        OutdoorContextView result = service.saveContext(
                OWNER_ID,
                TRACK_ID,
                command(true));

        assertThat(result.weather().status())
                .isEqualTo(WeatherStatus.AVAILABLE);
        assertThat(result.weather().maximumWindGustKilometersPerHour())
                .isEqualTo(58.0);

        verify(weatherProvider).forecast(eq(
                new WeatherForecastRequest(
                        45.8326,
                        6.8652,
                        Instant.parse("2026-08-31T06:00:00Z"),
                        Instant.parse("2026-08-31T12:00:00Z"))));
    }

    @Test
    void preservesPlanWhenForecastIsOutsideProviderHorizon() {
        prepareOwnedTrackAndPersistence();
        given(weatherProvider.forecast(any()))
                .willThrow(new ForecastOutsideHorizonException());

        OutdoorContextView result = service.saveContext(
                OWNER_ID,
                TRACK_ID,
                command(true));

        assertThat(result.weather().status())
                .isEqualTo(WeatherStatus.OUTSIDE_FORECAST_HORIZON);
        assertThat(result.daylight()).isNotNull();
    }

    @Test
    void rejectsAmbiguousDaylightSavingTime() {
        given(trackLocationService.requireOwnedStartPoint(
                OWNER_ID,
                TRACK_ID))
                .willReturn(START_POINT);

        OutdoorPlanCommand ambiguous = new OutdoorPlanCommand(
                LocalDateTime.parse("2026-10-25T02:30:00"),
                360,
                "Europe/Paris",
                false);

        assertThatThrownBy(() -> service.saveContext(
                OWNER_ID,
                TRACK_ID,
                ambiguous))
                .isInstanceOf(InvalidOutdoorContextException.class)
                .hasMessageContaining("ambiguë");

        verifyNoInteractions(contextRepository);
        verify(weatherProvider, never()).forecast(any());
    }

    @Test
    void doesNotRevealAnUnownedTrack() {
        given(trackLocationService.requireOwnedStartPoint(
                OWNER_ID,
                TRACK_ID))
                .willThrow(new TrackNotFoundException());

        assertThatThrownBy(() -> service.getContext(
                OWNER_ID,
                TRACK_ID))
                .isInstanceOf(TrackNotFoundException.class);

        verifyNoInteractions(contextRepository);
    }

    private void prepareOwnedTrackAndPersistence() {
        given(trackLocationService.requireOwnedStartPoint(
                OWNER_ID,
                TRACK_ID))
                .willReturn(START_POINT);
        given(contextRepository.findById(TRACK_ID))
                .willReturn(Optional.empty());
        given(contextRepository.save(any(TrackOutdoorContext.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(daylightCalculator.calculate(
                eq(START_POINT),
                any(),
                any(),
                any()))
                .willReturn(daylight());
    }

    private OutdoorPlanCommand command(boolean consent) {
        return new OutdoorPlanCommand(
                LocalDateTime.parse("2026-08-31T08:00:00"),
                360,
                "Europe/Paris",
                consent);
    }

    private DaylightWindow daylight() {
        return new DaylightWindow(
                Instant.parse("2026-08-31T04:51:00Z"),
                Instant.parse("2026-08-31T18:16:00Z"),
                Instant.parse("2026-08-31T04:19:00Z"),
                Instant.parse("2026-08-31T18:48:00Z"),
                360,
                0,
                DaylightCondition.NORMAL);
    }

    private WeatherForecast forecast() {
        return new WeatherForecast(
                "Open-Meteo",
                "https://open-meteo.com/",
                NOW,
                Instant.parse("2026-08-31T06:00:00Z"),
                Instant.parse("2026-08-31T13:00:00Z"),
                5.0,
                12.0,
                2.0,
                10.0,
                70,
                3.2,
                0.0,
                32.0,
                58.0,
                1_210.0);
    }
}
