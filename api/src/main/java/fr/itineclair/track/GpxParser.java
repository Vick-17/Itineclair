package fr.itineclair.track;

import java.io.InputStream;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
class GpxParser {

    private static final Set<String> SUPPORTED_NAMESPACES = Set.of(
            "http://www.topografix.com/GPX/1/0",
            "http://www.topografix.com/GPX/1/1");

    private final int maximumPointCount;

    GpxParser(
            @Value("${itineclair.track.max-points:50000}")
            int maximumPointCount) {
        if (maximumPointCount < 2) {
            throw new IllegalArgumentException(
                    "The GPX point limit must be at least two.");
        }
        this.maximumPointCount = maximumPointCount;
    }

    ParsedGpx parse(InputStream inputStream, String fallbackName) {
        XMLStreamReader reader = null;

        try {
            reader = secureXmlInputFactory()
                    .createXMLStreamReader(inputStream);

            ParseState state = new ParseState(
                    fallbackName,
                    maximumPointCount);

            while (reader.hasNext()) {
                int event = reader.next();

                switch (event) {
                    case XMLStreamConstants.START_ELEMENT ->
                            state.startElement(reader);
                    case XMLStreamConstants.CHARACTERS,
                            XMLStreamConstants.CDATA ->
                            state.characters(reader.getText());
                    case XMLStreamConstants.END_ELEMENT ->
                            state.endElement(reader);
                    case XMLStreamConstants.DTD,
                            XMLStreamConstants.ENTITY_REFERENCE ->
                            throw invalid(
                                    "Les déclarations DTD et entités XML "
                                            + "ne sont pas autorisées.");
                    default -> {
                        // Les commentaires et espaces hors éléments sont ignorés.
                    }
                }
            }

            return state.result();
        } catch (InvalidGpxException exception) {
            throw exception;
        } catch (XMLStreamException | IllegalArgumentException exception) {
            throw new InvalidGpxException(
                    "Le fichier n’est pas un document GPX valide.",
                    exception);
        } finally {
            closeQuietly(reader);
        }
    }

    private XMLInputFactory secureXmlInputFactory() {
        XMLInputFactory factory = XMLInputFactory.newFactory();

        try {
            factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
            factory.setProperty(
                    "javax.xml.stream.isSupportingExternalEntities",
                    false);
            factory.setProperty(
                    XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES,
                    false);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "The XML parser cannot be configured securely.",
                    exception);
        }

        factory.setXMLResolver(
                (publicId, systemId, baseUri, namespace) -> {
                    throw new XMLStreamException(
                            "External XML resources are disabled.");
                });

        return factory;
    }

    private void closeQuietly(XMLStreamReader reader) {
        if (reader == null) {
            return;
        }

        try {
            reader.close();
        } catch (XMLStreamException ignored) {
            // La fermeture ne doit pas masquer le résultat de l’analyse.
        }
    }

    private static InvalidGpxException invalid(String message) {
        return new InvalidGpxException(message);
    }

    private static final class ParseState {

        private static final int MAXIMUM_CAPTURED_TEXT_LENGTH = 1024;

        private final String fallbackName;
        private final int maximumPointCount;
        private final List<ParsedTrackPoint> points = new ArrayList<>();

        private String gpxNamespace;
        private String trackName;
        private boolean rootSeen;
        private boolean inTrack;
        private boolean inSegment;
        private boolean segmentHasPoint;
        private int segmentCount;
        private int currentSegmentNumber;
        private int currentPointNumber;
        private MutablePoint currentPoint;
        private CapturedValue capturedValue;
        private StringBuilder capturedText;

        private ParseState(
                String fallbackName,
                int maximumPointCount) {
            this.fallbackName = fallbackName;
            this.maximumPointCount = maximumPointCount;
        }

        private void startElement(XMLStreamReader reader) {
            if (!rootSeen) {
                readRoot(reader);
                return;
            }

            if (!isGpxElement(reader)) {
                return;
            }

            String localName = reader.getLocalName();

            switch (localName) {
                case "trk" -> startTrack();
                case "name" -> {
                    if (inTrack
                            && !inSegment
                            && trackName == null) {
                        startCapture(CapturedValue.TRACK_NAME);
                    }
                }
                case "trkseg" -> startSegment();
                case "trkpt" -> startPoint(reader);
                case "ele" -> {
                    if (currentPoint != null) {
                        if (currentPoint.elevation != null) {
                            throw invalid(
                                    "Un point GPX contient plusieurs altitudes.");
                        }
                        startCapture(CapturedValue.ELEVATION);
                    }
                }
                case "time" -> {
                    if (currentPoint != null) {
                        if (currentPoint.recordedAt != null) {
                            throw invalid(
                                    "Un point GPX contient plusieurs horodatages.");
                        }
                        startCapture(CapturedValue.TIME);
                    }
                }
                default -> {
                    // Les autres éléments GPX sont sans effet pour ce MVP.
                }
            }
        }

        private void characters(String text) {
            if (capturedText == null) {
                return;
            }

            if (capturedText.length() + text.length()
                    > MAXIMUM_CAPTURED_TEXT_LENGTH) {
                throw invalid(
                        "Une valeur textuelle du fichier GPX est trop longue.");
            }

            capturedText.append(text);
        }

        private void endElement(XMLStreamReader reader) {
            if (!rootSeen || !isGpxElement(reader)) {
                return;
            }

            String localName = reader.getLocalName();

            if (capturedValue != null
                    && capturedValue.elementName.equals(localName)) {
                finishCapture();
            }

            switch (localName) {
                case "trkpt" -> finishPoint();
                case "trkseg" -> finishSegment();
                case "trk" -> finishTrack();
                default -> {
                    // Aucun autre élément ne modifie l’état structurel.
                }
            }
        }

        private ParsedGpx result() {
            if (!rootSeen) {
                throw invalid("Le document XML ne contient aucune racine GPX.");
            }

            if (inTrack || inSegment || currentPoint != null) {
                throw invalid("La structure du fichier GPX est incomplète.");
            }

            if (points.size() < 2) {
                throw invalid(
                        "La trace GPX doit contenir au moins deux points.");
            }

            return new ParsedGpx(
                    cleanName(trackName, fallbackName),
                    segmentCount,
                    points);
        }

        private void readRoot(XMLStreamReader reader) {
            String namespace = Objects.requireNonNullElse(
                    reader.getNamespaceURI(),
                    "");

            if (!"gpx".equals(reader.getLocalName())
                    || !SUPPORTED_NAMESPACES.contains(namespace)) {
                throw invalid(
                        "Seuls les fichiers GPX 1.0 et 1.1 sont acceptés.");
            }

            rootSeen = true;
            gpxNamespace = namespace;
        }

        private boolean isGpxElement(XMLStreamReader reader) {
            return gpxNamespace.equals(
                    Objects.requireNonNullElse(
                            reader.getNamespaceURI(),
                            ""));
        }

        private void startTrack() {
            if (inTrack || inSegment || currentPoint != null) {
                throw invalid("La structure des traces GPX est invalide.");
            }
            inTrack = true;
        }

        private void finishTrack() {
            if (!inTrack || inSegment || currentPoint != null) {
                throw invalid("La structure des traces GPX est invalide.");
            }
            inTrack = false;
        }

        private void startSegment() {
            if (!inTrack || inSegment || currentPoint != null) {
                throw invalid("La structure des segments GPX est invalide.");
            }

            inSegment = true;
            segmentHasPoint = false;
            currentPointNumber = 0;
        }

        private void finishSegment() {
            if (!inSegment || currentPoint != null) {
                throw invalid("La structure des segments GPX est invalide.");
            }
            inSegment = false;
        }

        private void startPoint(XMLStreamReader reader) {
            if (!inSegment || currentPoint != null) {
                throw invalid(
                        "Un point de trace doit appartenir à un segment GPX.");
            }

            if (!segmentHasPoint) {
                currentSegmentNumber = segmentCount;
                segmentCount++;
                segmentHasPoint = true;
            }

            currentPoint = new MutablePoint(
                    coordinate(reader, "lat", -90.0, 90.0),
                    coordinate(reader, "lon", -180.0, 180.0));
        }

        private void finishPoint() {
            if (currentPoint == null) {
                throw invalid("La structure des points GPX est invalide.");
            }

            if (points.size() >= maximumPointCount) {
                throw invalid(
                        "La trace GPX contient plus de "
                                + maximumPointCount
                                + " points.");
            }

            points.add(new ParsedTrackPoint(
                    currentSegmentNumber,
                    currentPointNumber,
                    currentPoint.latitude,
                    currentPoint.longitude,
                    currentPoint.elevation,
                    currentPoint.recordedAt));

            currentPointNumber++;
            currentPoint = null;
        }

        private double coordinate(
                XMLStreamReader reader,
                String attributeName,
                double minimum,
                double maximum) {
            String rawValue = reader.getAttributeValue(
                    null,
                    attributeName);

            if (rawValue == null) {
                throw invalid(
                        "Un point GPX ne contient pas toutes ses coordonnées.");
            }

            double value;

            try {
                value = Double.parseDouble(rawValue);
            } catch (NumberFormatException exception) {
                throw new InvalidGpxException(
                        "Une coordonnée GPX n’est pas numérique.",
                        exception);
            }

            if (!Double.isFinite(value)
                    || value < minimum
                    || value > maximum) {
                throw invalid(
                        "Une coordonnée GPX est hors des limites terrestres.");
            }

            return value;
        }

        private void startCapture(CapturedValue value) {
            if (capturedValue != null) {
                throw invalid(
                        "La structure d’une valeur GPX est invalide.");
            }

            capturedValue = value;
            capturedText = new StringBuilder();
        }

        private void finishCapture() {
            String value = capturedText.toString().strip();

            switch (capturedValue) {
                case TRACK_NAME -> trackName = value;
                case ELEVATION -> currentPoint.elevation =
                        elevation(value);
                case TIME -> currentPoint.recordedAt =
                        recordedAt(value);
            }

            capturedValue = null;
            capturedText = null;
        }

        private Double elevation(String value) {
            double elevation;

            try {
                elevation = Double.parseDouble(value);
            } catch (NumberFormatException exception) {
                throw new InvalidGpxException(
                        "Une altitude GPX n’est pas numérique.",
                        exception);
            }

            if (!Double.isFinite(elevation)
                    || elevation < -12000.0
                    || elevation > 12000.0) {
                throw invalid(
                        "Une altitude GPX est hors des limites acceptées.");
            }

            return elevation;
        }

        private Instant recordedAt(String value) {
            try {
                return OffsetDateTime.parse(value).toInstant();
            } catch (DateTimeParseException exception) {
                throw new InvalidGpxException(
                        "Un horodatage GPX n’est pas valide.",
                        exception);
            }
        }

        private String cleanName(
                String candidate,
                String fallback) {
            String value = candidate == null || candidate.isBlank()
                    ? fallback
                    : candidate;

            String normalized = value
                    .replaceAll("\\p{Cntrl}", " ")
                    .replaceAll("\\s+", " ")
                    .strip();

            if (normalized.isEmpty()) {
                normalized = "Trace sans nom";
            }

            return normalized.length() <= 120
                    ? normalized
                    : normalized.substring(0, 120);
        }
    }

    private enum CapturedValue {
        TRACK_NAME("name"),
        ELEVATION("ele"),
        TIME("time");

        private final String elementName;

        CapturedValue(String elementName) {
            this.elementName = elementName;
        }
    }

    private static final class MutablePoint {

        private final double latitude;
        private final double longitude;
        private Double elevation;
        private Instant recordedAt;

        private MutablePoint(
                double latitude,
                double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }
}
