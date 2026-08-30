CREATE TABLE track_outdoor_contexts
(
    track_id                              UUID         PRIMARY KEY,
    planned_start_at                      TIMESTAMPTZ  NOT NULL,
    planned_duration_minutes              INTEGER      NOT NULL,
    time_zone                             VARCHAR(64)  NOT NULL,
    weather_consent_at                    TIMESTAMPTZ,
    weather_status                        VARCHAR(32)  NOT NULL,
    weather_source                        VARCHAR(80),
    weather_attribution_url               VARCHAR(255),
    weather_checked_at                    TIMESTAMPTZ,
    weather_valid_from                    TIMESTAMPTZ,
    weather_valid_until                   TIMESTAMPTZ,
    weather_minimum_temperature_celsius   DOUBLE PRECISION,
    weather_maximum_temperature_celsius   DOUBLE PRECISION,
    weather_minimum_apparent_celsius      DOUBLE PRECISION,
    weather_maximum_apparent_celsius      DOUBLE PRECISION,
    weather_maximum_precipitation_percent INTEGER,
    weather_precipitation_sum_mm          DOUBLE PRECISION,
    weather_snowfall_sum_cm               DOUBLE PRECISION,
    weather_maximum_wind_speed_kmh        DOUBLE PRECISION,
    weather_maximum_wind_gust_kmh         DOUBLE PRECISION,
    weather_model_elevation_meters        DOUBLE PRECISION,
    updated_at                            TIMESTAMPTZ  NOT NULL,

    CONSTRAINT fk_track_outdoor_context_track
        FOREIGN KEY (track_id)
        REFERENCES tracks (id)
        ON DELETE CASCADE,

    CONSTRAINT ck_track_outdoor_duration
        CHECK (planned_duration_minutes BETWEEN 30 AND 1440),

    CONSTRAINT ck_track_outdoor_time_zone
        CHECK (LENGTH(TRIM(time_zone)) BETWEEN 1 AND 64),

    CONSTRAINT ck_track_outdoor_weather_status
        CHECK (
            weather_status IN (
                'NOT_REQUESTED',
                'AVAILABLE',
                'OUTSIDE_FORECAST_HORIZON',
                'UNAVAILABLE'
            )
        ),

    CONSTRAINT ck_track_outdoor_weather_consent
        CHECK (
            (
                weather_status = 'NOT_REQUESTED'
                AND weather_consent_at IS NULL
                AND weather_source IS NULL
                AND weather_attribution_url IS NULL
                AND weather_checked_at IS NULL
            )
            OR
            (
                weather_status <> 'NOT_REQUESTED'
                AND weather_consent_at IS NOT NULL
                AND weather_source IS NOT NULL
                AND weather_attribution_url IS NOT NULL
                AND weather_checked_at IS NOT NULL
            )
        ),

    CONSTRAINT ck_track_outdoor_weather_window
        CHECK (
            weather_status <> 'AVAILABLE'
            OR (
                weather_valid_from IS NOT NULL
                AND weather_valid_until IS NOT NULL
                AND weather_valid_until > weather_valid_from
            )
        ),

    CONSTRAINT ck_track_outdoor_precipitation_probability
        CHECK (
            weather_maximum_precipitation_percent IS NULL
            OR weather_maximum_precipitation_percent BETWEEN 0 AND 100
        ),

    CONSTRAINT ck_track_outdoor_non_negative_weather
        CHECK (
            (
                weather_precipitation_sum_mm IS NULL
                OR weather_precipitation_sum_mm >= 0.0
            )
            AND (
                weather_snowfall_sum_cm IS NULL
                OR weather_snowfall_sum_cm >= 0.0
            )
            AND (
                weather_maximum_wind_speed_kmh IS NULL
                OR weather_maximum_wind_speed_kmh >= 0.0
            )
            AND (
                weather_maximum_wind_gust_kmh IS NULL
                OR weather_maximum_wind_gust_kmh >= 0.0
            )
        )
);
