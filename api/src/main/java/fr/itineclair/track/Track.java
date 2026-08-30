package fr.itineclair.track;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tracks")
class Track {

    @Id
    private UUID id;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "source_filename", nullable = false, length = 255)
    private String sourceFilename;

    @Column(name = "segment_count", nullable = false)
    private int segmentCount;

    @Column(name = "point_count", nullable = false)
    private int pointCount;

    @Column(name = "elevation_point_count", nullable = false)
    private int elevationPointCount;

    @Column(name = "facts_version")
    private Integer factsVersion;

    @Column(name = "total_distance_meters")
    private Double totalDistanceMeters;

    @Column(name = "elevation_gain_meters")
    private Double elevationGainMeters;

    @Column(name = "elevation_loss_meters")
    private Double elevationLossMeters;

    @Column(name = "minimum_elevation_meters")
    private Double minimumElevationMeters;

    @Column(name = "maximum_elevation_meters")
    private Double maximumElevationMeters;

    @Column(name = "maximum_uphill_grade_percent")
    private Double maximumUphillGradePercent;

    @Column(name = "maximum_downhill_grade_percent")
    private Double maximumDownhillGradePercent;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Track() {
        // Constructeur requis par JPA.
    }

    private Track(
            UUID id,
            UUID ownerId,
            String name,
            String sourceFilename,
            int segmentCount,
            int pointCount,
            int elevationPointCount,
            TrackFacts facts,
            Instant createdAt) {
        this.id = id;
        this.ownerId = ownerId;
        this.name = name;
        this.sourceFilename = sourceFilename;
        this.segmentCount = segmentCount;
        this.pointCount = pointCount;
        this.elevationPointCount = elevationPointCount;
        updateFacts(facts);
        this.createdAt = createdAt;
    }

    static Track create(
            UUID ownerId,
            String sourceFilename,
            ParsedGpx parsedGpx,
            TrackFacts facts) {
        return new Track(
                UUID.randomUUID(),
                ownerId,
                parsedGpx.name(),
                sourceFilename,
                parsedGpx.segmentCount(),
                parsedGpx.pointCount(),
                parsedGpx.elevationPointCount(),
                facts,
                Instant.now());
    }

    void updateFacts(TrackFacts facts) {
        this.factsVersion = facts.version();
        this.totalDistanceMeters =
                facts.totalDistanceMeters();
        this.elevationGainMeters =
                facts.elevationGainMeters();
        this.elevationLossMeters =
                facts.elevationLossMeters();
        this.minimumElevationMeters =
                facts.minimumElevationMeters();
        this.maximumElevationMeters =
                facts.maximumElevationMeters();
        this.maximumUphillGradePercent =
                facts.maximumUphillGradePercent();
        this.maximumDownhillGradePercent =
                facts.maximumDownhillGradePercent();
    }

    boolean needsFactsRefresh() {
        return !Integer.valueOf(TrackFacts.CURRENT_VERSION)
                .equals(factsVersion)
                || totalDistanceMeters == null;
    }

    UUID id() {
        return id;
    }

    String name() {
        return name;
    }

    String sourceFilename() {
        return sourceFilename;
    }

    int segmentCount() {
        return segmentCount;
    }

    int pointCount() {
        return pointCount;
    }

    int elevationPointCount() {
        return elevationPointCount;
    }

    Integer factsVersion() {
        return factsVersion;
    }

    Double totalDistanceMeters() {
        return totalDistanceMeters;
    }

    Double elevationGainMeters() {
        return elevationGainMeters;
    }

    Double elevationLossMeters() {
        return elevationLossMeters;
    }

    Double minimumElevationMeters() {
        return minimumElevationMeters;
    }

    Double maximumElevationMeters() {
        return maximumElevationMeters;
    }

    Double maximumUphillGradePercent() {
        return maximumUphillGradePercent;
    }

    Double maximumDownhillGradePercent() {
        return maximumDownhillGradePercent;
    }

    Instant createdAt() {
        return createdAt;
    }
}
