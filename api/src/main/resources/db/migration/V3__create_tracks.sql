CREATE TABLE tracks
(
    id                    UUID         PRIMARY KEY,
    owner_id              UUID         NOT NULL,
    name                  VARCHAR(120) NOT NULL,
    source_filename       VARCHAR(255) NOT NULL,
    segment_count         INTEGER      NOT NULL,
    point_count           INTEGER      NOT NULL,
    elevation_point_count INTEGER      NOT NULL,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_tracks_owner
        FOREIGN KEY (owner_id)
        REFERENCES user_accounts (id)
        ON DELETE CASCADE,

    CONSTRAINT ck_tracks_segment_count
        CHECK (segment_count BETWEEN 1 AND 1000),

    CONSTRAINT ck_tracks_point_count
        CHECK (point_count BETWEEN 2 AND 50000),

    CONSTRAINT ck_tracks_elevation_point_count
        CHECK (
            elevation_point_count BETWEEN 0 AND point_count
        )
);

CREATE INDEX idx_tracks_owner_created_at
    ON tracks (owner_id, created_at DESC);

CREATE TABLE track_points
(
    track_id       UUID             NOT NULL,
    segment_number INTEGER          NOT NULL,
    point_number   INTEGER          NOT NULL,
    latitude       DOUBLE PRECISION NOT NULL,
    longitude      DOUBLE PRECISION NOT NULL,
    elevation      DOUBLE PRECISION,
    recorded_at    TIMESTAMPTZ,

    PRIMARY KEY (track_id, segment_number, point_number),

    CONSTRAINT fk_track_points_track
        FOREIGN KEY (track_id)
        REFERENCES tracks (id)
        ON DELETE CASCADE,

    CONSTRAINT ck_track_points_segment_number
        CHECK (segment_number >= 0),

    CONSTRAINT ck_track_points_point_number
        CHECK (point_number >= 0),

    CONSTRAINT ck_track_points_latitude
        CHECK (latitude BETWEEN -90.0 AND 90.0),

    CONSTRAINT ck_track_points_longitude
        CHECK (longitude BETWEEN -180.0 AND 180.0),

    CONSTRAINT ck_track_points_elevation
        CHECK (
            elevation IS NULL
            OR elevation BETWEEN -12000.0 AND 12000.0
        )
);
