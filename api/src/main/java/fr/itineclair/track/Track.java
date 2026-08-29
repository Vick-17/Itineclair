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
            Instant createdAt) {
        this.id = id;
        this.ownerId = ownerId;
        this.name = name;
        this.sourceFilename = sourceFilename;
        this.segmentCount = segmentCount;
        this.pointCount = pointCount;
        this.elevationPointCount = elevationPointCount;
        this.createdAt = createdAt;
    }

    static Track create(
            UUID ownerId,
            String sourceFilename,
            ParsedGpx parsedGpx) {
        return new Track(
                UUID.randomUUID(),
                ownerId,
                parsedGpx.name(),
                sourceFilename,
                parsedGpx.segmentCount(),
                parsedGpx.pointCount(),
                parsedGpx.elevationPointCount(),
                Instant.now());
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

    Instant createdAt() {
        return createdAt;
    }
}
