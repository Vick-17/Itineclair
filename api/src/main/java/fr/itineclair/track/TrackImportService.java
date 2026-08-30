package fr.itineclair.track;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class TrackImportService {

    private static final long MAXIMUM_FILE_SIZE_BYTES =
            10L * 1024L * 1024L;

    private static final int POINT_BATCH_SIZE = 500;

    private static final String INSERT_POINT_SQL = """
            INSERT INTO track_points
            (
                track_id,
                segment_number,
                point_number,
                latitude,
                longitude,
                elevation,
                recorded_at
            )
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String SELECT_POINTS_SQL = """
            SELECT
                segment_number,
                point_number,
                latitude,
                longitude,
                elevation,
                recorded_at
            FROM track_points
            WHERE track_id = ?
            ORDER BY segment_number, point_number
            """;

    private final TrackRepository trackRepository;
    private final GpxParser gpxParser;
    private final TrackFactsCalculator trackFactsCalculator;
    private final JdbcTemplate jdbcTemplate;

    public TrackImportService(
            TrackRepository trackRepository,
            GpxParser gpxParser,
            TrackFactsCalculator trackFactsCalculator,
            JdbcTemplate jdbcTemplate) {
        this.trackRepository = trackRepository;
        this.gpxParser = gpxParser;
        this.trackFactsCalculator = trackFactsCalculator;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public TrackSummary importGpx(
            UUID ownerId,
            MultipartFile file) {
        Objects.requireNonNull(ownerId, "ownerId");

        String sourceFilename = validateAndCleanFilename(file);

        if (file.isEmpty()) {
            throw new InvalidGpxException(
                    "Le fichier GPX est vide.");
        }

        if (file.getSize() > MAXIMUM_FILE_SIZE_BYTES) {
            throw new GpxFileTooLargeException();
        }

        ParsedGpx parsedGpx;

        try (InputStream inputStream = file.getInputStream()) {
            parsedGpx = gpxParser.parse(
                    inputStream,
                    fallbackName(sourceFilename));
        } catch (IOException exception) {
            throw new InvalidGpxException(
                    "Le fichier GPX n’a pas pu être lu.",
                    exception);
        }

        TrackFacts facts =
                trackFactsCalculator.calculate(
                        parsedGpx.points());

        Track track = Track.create(
                ownerId,
                sourceFilename,
                parsedGpx,
                facts);

        trackRepository.saveAndFlush(track);
        persistPoints(track.id(), parsedGpx.points());

        return TrackSummary.from(track);
    }

    @Transactional
    public List<TrackSummary> listTracks(UUID ownerId) {
        Objects.requireNonNull(ownerId, "ownerId");

        List<Track> tracks = trackRepository
                .findAllByOwnerIdOrderByCreatedAtDesc(ownerId);

        tracks.stream()
                .filter(Track::needsFactsRefresh)
                .forEach(this::refreshFacts);

        return tracks.stream()
                .map(TrackSummary::from)
                .toList();
    }

    private void refreshFacts(Track track) {
        TrackFacts facts = trackFactsCalculator.calculate(
                loadPoints(track.id()));
        track.updateFacts(facts);
    }

    private List<ParsedTrackPoint> loadPoints(UUID trackId) {
        return jdbcTemplate.query(
                SELECT_POINTS_SQL,
                statement -> statement.setObject(1, trackId),
                (resultSet, rowNumber) -> {
                    double elevationValue =
                            resultSet.getDouble("elevation");

                    Double elevation = resultSet.wasNull()
                            ? null
                            : elevationValue;

                    OffsetDateTime recordedAt =
                            resultSet.getObject(
                                    "recorded_at",
                                    OffsetDateTime.class);

                    return new ParsedTrackPoint(
                            resultSet.getInt(
                                    "segment_number"),
                            resultSet.getInt(
                                    "point_number"),
                            resultSet.getDouble("latitude"),
                            resultSet.getDouble("longitude"),
                            elevation,
                            recordedAt == null
                                    ? null
                                    : recordedAt.toInstant());
                });
    }

    private void persistPoints(
            UUID trackId,
            List<ParsedTrackPoint> points) {
        jdbcTemplate.batchUpdate(
                INSERT_POINT_SQL,
                points,
                POINT_BATCH_SIZE,
                (statement, point) -> {
                    statement.setObject(1, trackId);
                    statement.setInt(
                            2,
                            point.segmentNumber());
                    statement.setInt(
                            3,
                            point.pointNumber());
                    statement.setDouble(
                            4,
                            point.latitude());
                    statement.setDouble(
                            5,
                            point.longitude());

                    if (point.elevation() == null) {
                        statement.setNull(6, Types.DOUBLE);
                    } else {
                        statement.setDouble(
                                6,
                                point.elevation());
                    }

                    if (point.recordedAt() == null) {
                        statement.setNull(
                                7,
                                Types.TIMESTAMP_WITH_TIMEZONE);
                    } else {
                        statement.setObject(
                                7,
                                OffsetDateTime.ofInstant(
                                        point.recordedAt(),
                                        ZoneOffset.UTC));
                    }
                });
    }

    private String validateAndCleanFilename(
            MultipartFile file) {
        if (file == null) {
            throw new InvalidGpxException(
                    "Aucun fichier GPX n’a été fourni.");
        }

        String originalFilename = Objects.requireNonNullElse(
                file.getOriginalFilename(),
                "");

        String normalizedSeparators = originalFilename
                .replace('\\', '/');

        String filename = normalizedSeparators.substring(
                normalizedSeparators.lastIndexOf('/') + 1)
                .replaceAll("\\p{Cntrl}", " ")
                .replaceAll("\\s+", " ")
                .strip();

        if (filename.isEmpty()) {
            throw new UnsupportedGpxFileException();
        }

        if (filename.length() > 255) {
            throw new InvalidGpxException(
                    "Le nom du fichier GPX est trop long.");
        }

        if (!filename
                .toLowerCase(Locale.ROOT)
                .endsWith(".gpx")) {
            throw new UnsupportedGpxFileException();
        }

        return filename;
    }

    private String fallbackName(String filename) {
        return filename.substring(0, filename.length() - 4);
    }
}
