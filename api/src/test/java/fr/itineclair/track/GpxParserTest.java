package fr.itineclair.track;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GpxParserTest {

    private final GpxParser parser = new GpxParser(50_000);

    @Test
    void parsesTrackNameSegmentsElevationAndTime() {
        ParsedGpx parsed = parse("""
                <?xml version="1.0" encoding="UTF-8"?>
                <gpx version="1.1"
                     creator="Itineclair test"
                     xmlns="http://www.topografix.com/GPX/1/1">
                  <trk>
                    <name>  Tour   du lac  </name>
                    <trkseg>
                      <trkpt lat="45.1000" lon="6.2000">
                        <ele>1200.5</ele>
                        <time>2026-08-29T08:00:00Z</time>
                      </trkpt>
                      <trkpt lat="45.1100" lon="6.2200">
                        <ele>1240.0</ele>
                      </trkpt>
                    </trkseg>
                    <trkseg>
                      <trkpt lat="45.1200" lon="6.2400"/>
                    </trkseg>
                  </trk>
                </gpx>
                """, "trace-importee");

        assertThat(parsed.name()).isEqualTo("Tour du lac");
        assertThat(parsed.segmentCount()).isEqualTo(2);
        assertThat(parsed.pointCount()).isEqualTo(3);
        assertThat(parsed.elevationPointCount()).isEqualTo(2);

        assertThat(parsed.points().getFirst())
                .satisfies(point -> {
                    assertThat(point.segmentNumber()).isZero();
                    assertThat(point.pointNumber()).isZero();
                    assertThat(point.latitude()).isEqualTo(45.1);
                    assertThat(point.longitude()).isEqualTo(6.2);
                    assertThat(point.elevation()).isEqualTo(1200.5);
                    assertThat(point.recordedAt()).isEqualTo(
                            Instant.parse("2026-08-29T08:00:00Z"));
                });

        assertThat(parsed.points().getLast())
                .satisfies(point -> {
                    assertThat(point.segmentNumber()).isEqualTo(1);
                    assertThat(point.pointNumber()).isZero();
                    assertThat(point.elevation()).isNull();
                });
    }

    @Test
    void usesFilenameWhenTrackHasNoName() {
        ParsedGpx parsed = parse("""
                <gpx version="1.0"
                     creator="test"
                     xmlns="http://www.topografix.com/GPX/1/0">
                  <trk>
                    <trkseg>
                      <trkpt lat="45.1" lon="6.2"/>
                      <trkpt lat="45.2" lon="6.3"/>
                    </trkseg>
                  </trk>
                </gpx>
                """, "boucle-belvedere");

        assertThat(parsed.name()).isEqualTo("boucle-belvedere");
    }

    @Test
    void rejectsDoctypeAndExternalEntity() {
        assertThatThrownBy(() -> parse("""
                <?xml version="1.0"?>
                <!DOCTYPE gpx [
                  <!ENTITY xxe SYSTEM "file:///etc/passwd">
                ]>
                <gpx version="1.1"
                     creator="test"
                     xmlns="http://www.topografix.com/GPX/1/1">
                  <trk>
                    <name>&xxe;</name>
                    <trkseg>
                      <trkpt lat="45.1" lon="6.2"/>
                      <trkpt lat="45.2" lon="6.3"/>
                    </trkseg>
                  </trk>
                </gpx>
                """, "trace"))
                .isInstanceOf(InvalidGpxException.class)
                .hasMessageNotContaining("/etc/passwd");
    }

    @Test
    void rejectsCoordinateOutsideEarthBounds() {
        assertThatThrownBy(() -> parse("""
                <gpx version="1.1"
                     creator="test"
                     xmlns="http://www.topografix.com/GPX/1/1">
                  <trk>
                    <trkseg>
                      <trkpt lat="91" lon="6.2"/>
                      <trkpt lat="45.2" lon="6.3"/>
                    </trkseg>
                  </trk>
                </gpx>
                """, "trace"))
                .isInstanceOf(InvalidGpxException.class)
                .hasMessageContaining("limites terrestres");
    }

    @Test
    void rejectsMorePointsThanConfigured() {
        GpxParser limitedParser = new GpxParser(2);

        assertThatThrownBy(() -> parse(limitedParser, """
                <gpx version="1.1"
                     creator="test"
                     xmlns="http://www.topografix.com/GPX/1/1">
                  <trk>
                    <trkseg>
                      <trkpt lat="45.1" lon="6.1"/>
                      <trkpt lat="45.2" lon="6.2"/>
                      <trkpt lat="45.3" lon="6.3"/>
                    </trkseg>
                  </trk>
                </gpx>
                """, "trace"))
                .isInstanceOf(InvalidGpxException.class)
                .hasMessageContaining("plus de 2 points");
    }

    @Test
    void rejectsDocumentWithoutTrackPoints() {
        assertThatThrownBy(() -> parse("""
                <gpx version="1.1"
                     creator="test"
                     xmlns="http://www.topografix.com/GPX/1/1">
                  <metadata><name>Une collection de points</name></metadata>
                  <wpt lat="45.1" lon="6.1"/>
                </gpx>
                """, "trace"))
                .isInstanceOf(InvalidGpxException.class)
                .hasMessageContaining("au moins deux points");
    }

    private ParsedGpx parse(
            String xml,
            String fallbackName) {
        return parse(parser, xml, fallbackName);
    }

    private ParsedGpx parse(
            GpxParser selectedParser,
            String xml,
            String fallbackName) {
        return selectedParser.parse(
                new ByteArrayInputStream(
                        xml.getBytes(StandardCharsets.UTF_8)),
                fallbackName);
    }
}
