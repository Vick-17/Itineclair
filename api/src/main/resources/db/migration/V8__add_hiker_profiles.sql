CREATE TABLE hiker_profiles
(
    account_id                         UUID         PRIMARY KEY,
    experience_level                   VARCHAR(24)  NOT NULL,
    usual_duration_minutes             INTEGER,
    usual_distance_meters              INTEGER,
    usual_elevation_gain_meters        INTEGER,
    created_at                         TIMESTAMPTZ  NOT NULL,
    updated_at                         TIMESTAMPTZ  NOT NULL,

    CONSTRAINT fk_hiker_profile_account FOREIGN KEY (account_id)
        REFERENCES user_accounts (id) ON DELETE CASCADE,

    CONSTRAINT ck_hiker_profile_experience CHECK (
        experience_level IN (
            'DISCOVERING',
            'OCCASIONAL',
            'REGULAR',
            'EXPERIENCED'
        )
    ),

    CONSTRAINT ck_hiker_profile_duration CHECK (
        usual_duration_minutes IS NULL
        OR usual_duration_minutes BETWEEN 15 AND 1440
    ),

    CONSTRAINT ck_hiker_profile_distance CHECK (
        usual_distance_meters IS NULL
        OR usual_distance_meters BETWEEN 500 AND 100000
    ),

    CONSTRAINT ck_hiker_profile_elevation_gain CHECK (
        usual_elevation_gain_meters IS NULL
        OR usual_elevation_gain_meters BETWEEN 0 AND 10000
    )
);
