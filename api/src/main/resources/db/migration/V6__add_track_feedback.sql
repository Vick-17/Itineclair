CREATE TABLE track_feedbacks
(
    track_id                 UUID         PRIMARY KEY,
    outcome                  VARCHAR(32)  NOT NULL,
    actual_duration_minutes  INTEGER,
    perceived_effort         INTEGER,
    conditions_comparison    VARCHAR(32)  NOT NULL,
    created_at               TIMESTAMPTZ  NOT NULL,
    updated_at               TIMESTAMPTZ  NOT NULL,

    CONSTRAINT fk_track_feedback_track FOREIGN KEY (track_id)
        REFERENCES tracks (id) ON DELETE CASCADE,
    CONSTRAINT ck_track_feedback_outcome CHECK (outcome IN (
        'COMPLETED_AS_PLANNED', 'COMPLETED_WITH_CHANGES',
        'TURNED_BACK', 'NOT_STARTED')),
    CONSTRAINT ck_track_feedback_duration CHECK (
        actual_duration_minutes IS NULL OR actual_duration_minutes BETWEEN 1 AND 1440),
    CONSTRAINT ck_track_feedback_effort CHECK (
        perceived_effort IS NULL OR perceived_effort BETWEEN 1 AND 5),
    CONSTRAINT ck_track_feedback_conditions CHECK (conditions_comparison IN (
        'BETTER_THAN_EXPECTED', 'AS_EXPECTED',
        'WORSE_THAN_EXPECTED', 'NOT_COMPARED')),
    CONSTRAINT ck_track_feedback_not_started CHECK (
        outcome <> 'NOT_STARTED' OR (
            actual_duration_minutes IS NULL
            AND perceived_effort IS NULL
            AND conditions_comparison = 'NOT_COMPARED'))
);

CREATE TABLE track_feedback_issues
(
    track_id UUID        NOT NULL,
    issue    VARCHAR(24) NOT NULL,
    PRIMARY KEY (track_id, issue),
    CONSTRAINT fk_track_feedback_issue_feedback FOREIGN KEY (track_id)
        REFERENCES track_feedbacks (track_id) ON DELETE CASCADE,
    CONSTRAINT ck_track_feedback_issue CHECK (issue IN (
        'WEATHER', 'TERRAIN', 'FATIGUE', 'NAVIGATION', 'EQUIPMENT'))
);
