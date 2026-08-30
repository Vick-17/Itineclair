package fr.itineclair.track;

import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class TrackLocationService {

    private static final String SELECT_OWNED_START_POINT_SQL = """
            SELECT
                point.latitude,
                point.longitude,
                point.elevation
            FROM tracks track
            JOIN track_points point
                ON point.track_id = track.id
            WHERE track.id = ?
              AND track.owner_id = ?
            ORDER BY point.segment_number, point.point_number
            LIMIT 1
            """;

    private final JdbcTemplate jdbcTemplate;

    public TrackLocationService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public TrackStartPoint requireOwnedStartPoint(
            UUID ownerId,
            UUID trackId) {
        return jdbcTemplate.query(
                        SELECT_OWNED_START_POINT_SQL,
                        statement -> {
                            statement.setObject(1, trackId);
                            statement.setObject(2, ownerId);
                        },
                        (resultSet, rowNumber) -> {
                            double elevationValue =
                                    resultSet.getDouble("elevation");
                            Double elevation = resultSet.wasNull()
                                    ? null
                                    : elevationValue;

                            return new TrackStartPoint(
                                    resultSet.getDouble("latitude"),
                                    resultSet.getDouble("longitude"),
                                    elevation);
                        })
                .stream()
                .findFirst()
                .orElseThrow(TrackNotFoundException::new);
    }
}
