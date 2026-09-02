package fr.itineclair.privacy;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.OutputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fr.itineclair.identity.AccountPrincipal;
import fr.itineclair.security.CurrentPasswordVerifier;
import tools.jackson.databind.json.JsonMapper;

@Service
public class AccountDataExportService {

    private static final String GPX_NAMESPACE =
            "http://www.topografix.com/GPX/1/1";

    private static final DateTimeFormatter FILENAME_TIMESTAMP =
            DateTimeFormatter
                    .ofPattern("uuuuMMdd'T'HHmmss'Z'", Locale.ROOT)
                    .withZone(ZoneOffset.UTC);

    private final AccountDataStore accountDataStore;
    private final CurrentPasswordVerifier currentPasswordVerifier;
    private final JsonMapper jsonMapper;
    private final Clock clock;
    private final XMLOutputFactory xmlOutputFactory;

    public AccountDataExportService(
            AccountDataStore accountDataStore,
            CurrentPasswordVerifier currentPasswordVerifier,
            JsonMapper jsonMapper,
            Clock clock) {
        this(
                accountDataStore,
                currentPasswordVerifier,
                jsonMapper,
                clock,
                secureXmlOutputFactory());
    }

    AccountDataExportService(
            AccountDataStore accountDataStore,
            CurrentPasswordVerifier currentPasswordVerifier,
            JsonMapper jsonMapper,
            Clock clock,
            XMLOutputFactory xmlOutputFactory) {
        this.accountDataStore = accountDataStore;
        this.currentPasswordVerifier = currentPasswordVerifier;
        this.jsonMapper = jsonMapper;
        this.clock = clock;
        this.xmlOutputFactory = xmlOutputFactory;
    }

    @Transactional(readOnly = true)
    public PreparedAccountExport prepareExport(
            AccountPrincipal principal,
            String rawPassword) {
        Objects.requireNonNull(principal, "principal is required.");

        if (!currentPasswordVerifier.matches(principal, rawPassword)) {
            throw new InvalidCurrentPasswordException();
        }

        AccountExportSnapshot snapshot = accountDataStore
                .loadSnapshot(principal.id())
                .orElseThrow(AccountDataUnavailableException::new);

        Instant exportedAt = clock.instant();
        String filename = "itineclair-export-"
                + FILENAME_TIMESTAMP.format(exportedAt)
                + ".zip";

        return new PreparedAccountExport(
                filename,
                exportedAt,
                snapshot);
    }

    @Transactional(readOnly = true)
    public void writeExport(
            PreparedAccountExport preparedExport,
            OutputStream outputStream)
            throws IOException {
        Objects.requireNonNull(
                preparedExport,
                "preparedExport is required.");
        Objects.requireNonNull(outputStream, "outputStream is required.");

        ZipOutputStream zip = new ZipOutputStream(outputStream, UTF_8);

        writeManifest(preparedExport, zip);

        for (AccountExportSnapshot.Track track
                : preparedExport.snapshot().tracks()) {
            List<AccountExportSnapshot.TrackPoint> points =
                    accountDataStore.loadTrackPoints(
                            preparedExport.snapshot().account().id(),
                            track.id());

            if (points.size() != track.pointCount()) {
                throw new IOException(
                        "The exported track points are inconsistent.");
            }

            writeGpx(track, points, preparedExport.exportedAt(), zip);
        }

        zip.finish();
        zip.flush();
    }

    private void writeManifest(
            PreparedAccountExport preparedExport,
            ZipOutputStream zip)
            throws IOException {
        ZipEntry entry = new ZipEntry("manifest.json");
        entry.setTime(preparedExport.exportedAt().toEpochMilli());
        zip.putNextEntry(entry);
        zip.write(jsonMapper.writeValueAsBytes(
                AccountDataExportManifest.from(preparedExport)));
        zip.closeEntry();
    }

    private void writeGpx(
            AccountExportSnapshot.Track track,
            List<AccountExportSnapshot.TrackPoint> points,
            Instant exportedAt,
            ZipOutputStream zip)
            throws IOException {
        ZipEntry entry = new ZipEntry(track.gpxFile());
        entry.setTime(exportedAt.toEpochMilli());
        zip.putNextEntry(entry);

        try {
            XMLStreamWriter xml = xmlOutputFactory.createXMLStreamWriter(
                    zip,
                    UTF_8.name());
            writeGpxDocument(xml, track, points);
            xml.flush();
        } catch (XMLStreamException exception) {
            throw new IOException(
                    "The GPX export could not be generated.",
                    exception);
        }

        zip.closeEntry();
    }

    private void writeGpxDocument(
            XMLStreamWriter xml,
            AccountExportSnapshot.Track track,
            List<AccountExportSnapshot.TrackPoint> points)
            throws XMLStreamException {
        xml.writeStartDocument(UTF_8.name(), "1.0");
        xml.writeStartElement("gpx");
        xml.writeDefaultNamespace(GPX_NAMESPACE);
        xml.writeAttribute("version", "1.1");
        xml.writeAttribute("creator", "Itineclair");

        xml.writeStartElement("metadata");
        xml.writeStartElement("name");
        xml.writeCharacters(track.name());
        xml.writeEndElement();
        xml.writeEndElement();

        xml.writeStartElement("trk");
        xml.writeStartElement("name");
        xml.writeCharacters(track.name());
        xml.writeEndElement();

        Integer currentSegment = null;

        for (AccountExportSnapshot.TrackPoint point : points) {
            if (!Integer.valueOf(point.segmentNumber())
                    .equals(currentSegment)) {
                if (currentSegment != null) {
                    xml.writeEndElement();
                }
                xml.writeStartElement("trkseg");
                currentSegment = point.segmentNumber();
            }

            writeTrackPoint(xml, point);
        }

        if (currentSegment != null) {
            xml.writeEndElement();
        }

        xml.writeEndElement();
        xml.writeEndElement();
        xml.writeEndDocument();
    }

    private void writeTrackPoint(
            XMLStreamWriter xml,
            AccountExportSnapshot.TrackPoint point)
            throws XMLStreamException {
        xml.writeStartElement("trkpt");
        xml.writeAttribute("lat", Double.toString(point.latitude()));
        xml.writeAttribute("lon", Double.toString(point.longitude()));

        if (point.elevation() != null) {
            xml.writeStartElement("ele");
            xml.writeCharacters(Double.toString(point.elevation()));
            xml.writeEndElement();
        }

        if (point.recordedAt() != null) {
            xml.writeStartElement("time");
            xml.writeCharacters(point.recordedAt().toString());
            xml.writeEndElement();
        }

        xml.writeEndElement();
    }

    private static XMLOutputFactory secureXmlOutputFactory() {
        XMLOutputFactory factory = XMLOutputFactory.newFactory();
        factory.setProperty(
                XMLOutputFactory.IS_REPAIRING_NAMESPACES,
                false);
        return factory;
    }
}
