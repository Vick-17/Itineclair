ALTER TABLE tracks
    ADD CONSTRAINT uk_tracks_id_owner
        UNIQUE (id, owner_id);

CREATE TABLE track_shares
(
    track_id    UUID        PRIMARY KEY,
    owner_id    UUID        NOT NULL,
    token_hash  VARCHAR(64) NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL,

    CONSTRAINT uk_track_shares_token_hash
        UNIQUE (token_hash),

    CONSTRAINT fk_track_shares_owned_track
        FOREIGN KEY (track_id, owner_id)
        REFERENCES tracks (id, owner_id)
        ON DELETE CASCADE,

    CONSTRAINT ck_track_shares_token_hash
        CHECK (token_hash ~ '^[0-9a-f]{64}$'),

    CONSTRAINT ck_track_shares_expiration
        CHECK (expires_at > created_at)
);

CREATE INDEX idx_track_shares_owner
    ON track_shares (owner_id);
