package fr.itineclair.privacy;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class JdbcAccountDataStore implements AccountDataStore {

    private static final String SELECT_ACCOUNT_SQL = """
            SELECT id, email, created_at
            FROM user_accounts
            WHERE id = ?
            """;

    private static final String SELECT_HIKER_PROFILE_SQL = """
            SELECT
                experience_level,
                usual_duration_minutes,
                usual_distance_meters,
                usual_elevation_gain_meters,
                created_at,
                updated_at
            FROM hiker_profiles
            WHERE account_id = ?
            """;

    private static final String SELECT_TRACKS_SQL = """
            SELECT
                t.id,
                t.name,
                t.source_filename,
                t.segment_count,
                t.point_count,
                t.elevation_point_count,
                t.created_at,
                t.facts_version,
                t.total_distance_meters,
                t.elevation_gain_meters,
                t.elevation_loss_meters,
                t.minimum_elevation_meters,
                t.maximum_elevation_meters,
                t.maximum_uphill_grade_percent,
                t.maximum_downhill_grade_percent,
                oc.track_id AS outdoor_track_id,
                oc.planned_start_at,
                oc.planned_duration_minutes,
                oc.time_zone,
                oc.weather_consent_at,
                oc.weather_status,
                oc.weather_source,
                oc.weather_attribution_url,
                oc.weather_checked_at,
                oc.weather_valid_from,
                oc.weather_valid_until,
                oc.weather_minimum_temperature_celsius,
                oc.weather_maximum_temperature_celsius,
                oc.weather_minimum_apparent_celsius,
                oc.weather_maximum_apparent_celsius,
                oc.weather_maximum_precipitation_percent,
                oc.weather_precipitation_sum_mm,
                oc.weather_snowfall_sum_cm,
                oc.weather_maximum_wind_speed_kmh,
                oc.weather_maximum_wind_gust_kmh,
                oc.weather_model_elevation_meters,
                oc.updated_at AS outdoor_updated_at,
                f.track_id AS feedback_track_id,
                f.outcome,
                f.actual_duration_minutes,
                f.perceived_effort,
                f.conditions_comparison,
                f.created_at AS feedback_created_at,
                f.updated_at AS feedback_updated_at,
                s.track_id AS share_track_id,
                s.created_at AS share_created_at,
                s.expires_at AS share_expires_at
            FROM tracks t
            LEFT JOIN track_outdoor_contexts oc ON oc.track_id = t.id
            LEFT JOIN track_feedbacks f ON f.track_id = t.id
            LEFT JOIN track_shares s
                ON s.track_id = t.id AND s.owner_id = t.owner_id
            WHERE t.owner_id = ?
            ORDER BY t.created_at DESC, t.id
            """;

    private static final String SELECT_FEEDBACK_ISSUES_SQL = """
            SELECT i.track_id, i.issue
            FROM track_feedback_issues i
            INNER JOIN tracks t ON t.id = i.track_id
            WHERE t.owner_id = ?
            ORDER BY i.track_id, i.issue
            """;

    private static final String SELECT_TRACK_POINTS_SQL = """
            SELECT
                p.segment_number,
                p.point_number,
                p.latitude,
                p.longitude,
                p.elevation,
                p.recorded_at
            FROM track_points p
            INNER JOIN tracks t ON t.id = p.track_id
            WHERE p.track_id = ? AND t.owner_id = ?
            ORDER BY p.segment_number, p.point_number
            """;

    private static final String DELETE_ACCOUNT_SQL = """
            DELETE FROM user_accounts
            WHERE id = ?
            """;

    private final JdbcTemplate jdbcTemplate;

    JdbcAccountDataStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<AccountExportSnapshot> loadSnapshot(UUID accountId) {
        List<AccountExportSnapshot.Account> accounts = jdbcTemplate.query(
                SELECT_ACCOUNT_SQL,
                (resultSet, rowNumber) -> new AccountExportSnapshot.Account(
                        resultSet.getObject("id", UUID.class),
                        resultSet.getString("email"),
                        instant(resultSet, "created_at")),
                accountId);

        if (accounts.isEmpty()) {
            return Optional.empty();
        }

        AccountExportSnapshot.HikerProfile hikerProfile =
                loadHikerProfile(accountId);
        Map<UUID, List<String>> issues = loadFeedbackIssues(accountId);

        List<AccountExportSnapshot.Track> tracks = jdbcTemplate.query(
                SELECT_TRACKS_SQL,
                (resultSet, rowNumber) -> mapTrack(resultSet, issues),
                accountId);

        return Optional.of(new AccountExportSnapshot(
                accounts.getFirst(),
                hikerProfile,
                tracks));
    }

    @Override
    public List<AccountExportSnapshot.TrackPoint> loadTrackPoints(
            UUID accountId,
            UUID trackId) {
        return jdbcTemplate.query(
                SELECT_TRACK_POINTS_SQL,
                (resultSet, rowNumber) ->
                        new AccountExportSnapshot.TrackPoint(
                                resultSet.getInt("segment_number"),
                                resultSet.getInt("point_number"),
                                resultSet.getDouble("latitude"),
                                resultSet.getDouble("longitude"),
                                nullableDouble(resultSet, "elevation"),
                                instant(resultSet, "recorded_at")),
                trackId,
                accountId);
    }

    @Override
    public int deleteAccount(UUID accountId) {
        return jdbcTemplate.update(DELETE_ACCOUNT_SQL, accountId);
    }

    private AccountExportSnapshot.HikerProfile loadHikerProfile(
            UUID accountId) {
        List<AccountExportSnapshot.HikerProfile> profiles =
                jdbcTemplate.query(
                        SELECT_HIKER_PROFILE_SQL,
                        (resultSet, rowNumber) ->
                                new AccountExportSnapshot.HikerProfile(
                                        resultSet.getString(
                                                "experience_level"),
                                        nullableInteger(
                                                resultSet,
                                                "usual_duration_minutes"),
                                        nullableInteger(
                                                resultSet,
                                                "usual_distance_meters"),
                                        nullableInteger(
                                                resultSet,
                                                "usual_elevation_gain_meters"),
                                        instant(resultSet, "created_at"),
                                        instant(resultSet, "updated_at")),
                        accountId);

        return profiles.isEmpty() ? null : profiles.getFirst();
    }

    private Map<UUID, List<String>> loadFeedbackIssues(UUID accountId) {
        Map<UUID, List<String>> issues = new HashMap<>();

        jdbcTemplate.query(
                SELECT_FEEDBACK_ISSUES_SQL,
                resultSet -> {
                    issues.computeIfAbsent(
                            resultSet.getObject("track_id", UUID.class),
                            ignored -> new ArrayList<>())
                            .add(resultSet.getString("issue"));
                },
                accountId);

        return issues;
    }

    private AccountExportSnapshot.Track mapTrack(
            ResultSet resultSet,
            Map<UUID, List<String>> issues)
            throws SQLException {
        UUID trackId = resultSet.getObject("id", UUID.class);

        return new AccountExportSnapshot.Track(
                trackId,
                resultSet.getString("name"),
                resultSet.getString("source_filename"),
                resultSet.getInt("segment_count"),
                resultSet.getInt("point_count"),
                resultSet.getInt("elevation_point_count"),
                instant(resultSet, "created_at"),
                mapFacts(resultSet),
                mapOutdoorContext(resultSet),
                mapFeedback(resultSet, issues.getOrDefault(
                        trackId,
                        List.of())),
                mapPrivateShare(resultSet),
                "tracks/" + trackId + ".gpx");
    }

    private AccountExportSnapshot.TrackFacts mapFacts(ResultSet resultSet)
            throws SQLException {
        Integer version = nullableInteger(resultSet, "facts_version");

        if (version == null) {
            return null;
        }

        return new AccountExportSnapshot.TrackFacts(
                version,
                resultSet.getDouble("total_distance_meters"),
                nullableDouble(resultSet, "elevation_gain_meters"),
                nullableDouble(resultSet, "elevation_loss_meters"),
                nullableDouble(resultSet, "minimum_elevation_meters"),
                nullableDouble(resultSet, "maximum_elevation_meters"),
                nullableDouble(
                        resultSet,
                        "maximum_uphill_grade_percent"),
                nullableDouble(
                        resultSet,
                        "maximum_downhill_grade_percent"));
    }

    private AccountExportSnapshot.OutdoorContext mapOutdoorContext(
            ResultSet resultSet)
            throws SQLException {
        if (resultSet.getObject("outdoor_track_id") == null) {
            return null;
        }

        return new AccountExportSnapshot.OutdoorContext(
                instant(resultSet, "planned_start_at"),
                resultSet.getInt("planned_duration_minutes"),
                resultSet.getString("time_zone"),
                instant(resultSet, "weather_consent_at"),
                resultSet.getString("weather_status"),
                resultSet.getString("weather_source"),
                resultSet.getString("weather_attribution_url"),
                instant(resultSet, "weather_checked_at"),
                instant(resultSet, "weather_valid_from"),
                instant(resultSet, "weather_valid_until"),
                nullableDouble(
                        resultSet,
                        "weather_minimum_temperature_celsius"),
                nullableDouble(
                        resultSet,
                        "weather_maximum_temperature_celsius"),
                nullableDouble(
                        resultSet,
                        "weather_minimum_apparent_celsius"),
                nullableDouble(
                        resultSet,
                        "weather_maximum_apparent_celsius"),
                nullableInteger(
                        resultSet,
                        "weather_maximum_precipitation_percent"),
                nullableDouble(
                        resultSet,
                        "weather_precipitation_sum_mm"),
                nullableDouble(resultSet, "weather_snowfall_sum_cm"),
                nullableDouble(
                        resultSet,
                        "weather_maximum_wind_speed_kmh"),
                nullableDouble(
                        resultSet,
                        "weather_maximum_wind_gust_kmh"),
                nullableDouble(
                        resultSet,
                        "weather_model_elevation_meters"),
                instant(resultSet, "outdoor_updated_at"));
    }

    private AccountExportSnapshot.Feedback mapFeedback(
            ResultSet resultSet,
            List<String> issues)
            throws SQLException {
        if (resultSet.getObject("feedback_track_id") == null) {
            return null;
        }

        return new AccountExportSnapshot.Feedback(
                resultSet.getString("outcome"),
                nullableInteger(resultSet, "actual_duration_minutes"),
                nullableInteger(resultSet, "perceived_effort"),
                resultSet.getString("conditions_comparison"),
                issues,
                instant(resultSet, "feedback_created_at"),
                instant(resultSet, "feedback_updated_at"));
    }

    private AccountExportSnapshot.PrivateShare mapPrivateShare(
            ResultSet resultSet)
            throws SQLException {
        if (resultSet.getObject("share_track_id") == null) {
            return null;
        }

        return new AccountExportSnapshot.PrivateShare(
                instant(resultSet, "share_created_at"),
                instant(resultSet, "share_expires_at"));
    }

    private static Integer nullableInteger(
            ResultSet resultSet,
            String column)
            throws SQLException {
        int value = resultSet.getInt(column);
        return resultSet.wasNull() ? null : value;
    }

    private static Double nullableDouble(
            ResultSet resultSet,
            String column)
            throws SQLException {
        double value = resultSet.getDouble(column);
        return resultSet.wasNull() ? null : value;
    }

    private static Instant instant(ResultSet resultSet, String column)
            throws SQLException {
        OffsetDateTime value = resultSet.getObject(
                column,
                OffsetDateTime.class);
        return value == null ? null : value.toInstant();
    }
}
