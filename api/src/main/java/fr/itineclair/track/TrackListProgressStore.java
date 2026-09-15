package fr.itineclair.track;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class TrackListProgressStore {

    private static final String SELECT_PROGRESS_SQL = """
            SELECT
                tracks.id AS track_id,
                outdoor.planned_start_at,
                outdoor.planned_duration_minutes,
                feedback.track_id IS NOT NULL AS feedback_recorded
            FROM tracks
            LEFT JOIN track_outdoor_contexts outdoor
                ON outdoor.track_id = tracks.id
            LEFT JOIN track_feedbacks feedback
                ON feedback.track_id = tracks.id
            WHERE tracks.owner_id = ?
            """;

    private final JdbcTemplate jdbcTemplate;

    TrackListProgressStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    Map<UUID, TrackListProgress> findAllByOwnerId(UUID ownerId) {
        Map<UUID, TrackListProgress> progressByTrack =
                new LinkedHashMap<>();

        jdbcTemplate.query(
                SELECT_PROGRESS_SQL,
                statement -> statement.setObject(1, ownerId),
                resultSet -> {
                    OffsetDateTime plannedStart = resultSet.getObject(
                            "planned_start_at",
                            OffsetDateTime.class);

                    int durationValue = resultSet.getInt(
                            "planned_duration_minutes");
                    Integer plannedDuration = resultSet.wasNull()
                            ? null
                            : durationValue;

                    progressByTrack.put(
                            resultSet.getObject("track_id", UUID.class),
                            new TrackListProgress(
                                    plannedStart == null
                                            ? null
                                            : plannedStart.toInstant(),
                                    plannedDuration,
                                    resultSet.getBoolean(
                                            "feedback_recorded")));
                });

        return progressByTrack;
    }
}
