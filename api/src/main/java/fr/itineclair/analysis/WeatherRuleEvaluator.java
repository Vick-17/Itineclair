package fr.itineclair.analysis;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import fr.itineclair.outdoor.WeatherStatus;
import fr.itineclair.outdoor.WeatherSummary;
import fr.itineclair.track.TrackSummary;

import static fr.itineclair.analysis.AnalysisRuleSupport.addHighThresholdFinding;
import static fr.itineclair.analysis.AnalysisRuleSupport.addOptionalHighThresholdFinding;
import static fr.itineclair.analysis.AnalysisRuleSupport.addOptionalLowThresholdFinding;
import static fr.itineclair.analysis.AnalysisRuleSupport.atLeast;
import static fr.itineclair.analysis.AnalysisRuleSupport.evidence;

final class WeatherRuleEvaluator {

    private static final double WEATHER_STALE_CAUTION_HOURS = 6.0;
    private static final double WEATHER_STALE_STRONG_HOURS = 12.0;
    private static final double MODEL_GAP_CAUTION_METERS = 500.0;
    private static final double MODEL_GAP_STRONG_METERS = 1_000.0;
    private static final double GUST_CAUTION_KILOMETERS_PER_HOUR = 50.0;
    private static final double GUST_STRONG_KILOMETERS_PER_HOUR = 70.0;
    private static final double PRECIPITATION_CAUTION_MILLIMETERS = 5.0;
    private static final double PRECIPITATION_STRONG_MILLIMETERS = 15.0;
    private static final double PRECIPITATION_CAUTION_PERCENT = 50.0;
    private static final double PRECIPITATION_STRONG_PERCENT = 80.0;
    private static final double SNOW_CAUTION_CENTIMETERS = 0.01;
    private static final double SNOW_STRONG_CENTIMETERS = 5.0;
    private static final double COLD_CAUTION_CELSIUS = 5.0;
    private static final double COLD_STRONG_CELSIUS = 0.0;
    private static final double HEAT_CAUTION_CELSIUS = 30.0;
    private static final double HEAT_STRONG_CELSIUS = 35.0;

    void evaluate(
            TrackSummary track,
            WeatherSummary weather,
            Instant generatedAt,
            List<AnalysisFinding> findings) {
        if (weather.status() != WeatherStatus.AVAILABLE) {
            findings.add(unavailableFinding(weather.status()));
            return;
        }

        analyzeFreshness(weather, generatedAt, findings);
        analyzeModelElevationGap(track, weather, findings);

        addOptionalHighThresholdFinding(
                findings,
                "STRONG_GUSTS_AT_START",
                AnalysisCategory.WEATHER,
                "Rafales marquées prévues au point de départ",
                "La rafale maximale est une prévision ponctuelle ; le vent peut être différent ou renforcé en crête et dans les passages exposés.",
                "Consulte le bulletin montagne et la Vigilance, puis réévalue les passages exposés et l’itinéraire de repli.",
                "maximumWindGustKilometersPerHour",
                "Rafale maximale",
                weather.maximumWindGustKilometersPerHour(),
                "km/h",
                GUST_CAUTION_KILOMETERS_PER_HOUR,
                GUST_STRONG_KILOMETERS_PER_HOUR);

        analyzePrecipitation(weather, findings);

        addOptionalHighThresholdFinding(
                findings,
                "SNOWFALL_AT_START",
                AnalysisCategory.WEATHER,
                "Neige prévue au point de départ",
                "La neige prévue au départ peut signaler des conditions différentes sur le parcours et ne remplace pas un bulletin neige ou avalanche.",
                "Consulte les bulletins officiels adaptés au secteur et vérifie terrain, équipement et compétences.",
                "snowfallSumCentimeters",
                "Neige cumulée",
                weather.snowfallSumCentimeters(),
                "cm",
                SNOW_CAUTION_CENTIMETERS,
                SNOW_STRONG_CENTIMETERS);

        addOptionalLowThresholdFinding(
                findings,
                "LOW_APPARENT_TEMPERATURE",
                AnalysisCategory.WEATHER,
                "Ressenti froid prévu au point de départ",
                "Le minimum ressenti est ponctuel et peut être plus bas avec l’altitude, le vent ou l’humidité.",
                "Vérifie le bulletin montagne et adapte vêtements, protections et solution de repli au groupe.",
                "minimumApparentCelsius",
                "Ressenti minimal",
                weather.minimumApparentCelsius(),
                "°C",
                COLD_CAUTION_CELSIUS,
                COLD_STRONG_CELSIUS);

        addOptionalHighThresholdFinding(
                findings,
                "HIGH_TEMPERATURE_AT_START",
                AnalysisCategory.WEATHER,
                "Chaleur marquée prévue au point de départ",
                "La température maximale ponctuelle peut accroître la charge thermique pendant l’effort.",
                "Consulte la Vigilance, privilégie les heures fraîches et adapte eau, rythme et possibilité de renoncer.",
                "maximumTemperatureCelsius",
                "Température maximale",
                weather.maximumTemperatureCelsius(),
                "°C",
                HEAT_CAUTION_CELSIUS,
                HEAT_STRONG_CELSIUS);
    }

    private void analyzeFreshness(
            WeatherSummary weather,
            Instant generatedAt,
            List<AnalysisFinding> findings) {
        if (weather.checkedAt() == null) {
            return;
        }

        double ageHours = Math.max(
                0.0,
                Duration.between(weather.checkedAt(), generatedAt)
                        .toMinutes() / 60.0);
        addHighThresholdFinding(
                findings,
                "WEATHER_FORECAST_STALE",
                AnalysisCategory.DATA_QUALITY,
                "Prévision à actualiser",
                "La prévision conservée vieillit et peut ne plus représenter l’évolution la plus récente.",
                "Enregistre de nouveau le contexte, puis consulte aussi les bulletins et alertes officiels.",
                "weatherAgeHours",
                "Âge de la prévision",
                ageHours,
                "h",
                WEATHER_STALE_CAUTION_HOURS,
                WEATHER_STALE_STRONG_HOURS);
    }

    private void analyzeModelElevationGap(
            TrackSummary track,
            WeatherSummary weather,
            List<AnalysisFinding> findings) {
        if (track.maximumElevationMeters() == null
                || weather.modelElevationMeters() == null) {
            return;
        }

        addHighThresholdFinding(
                findings,
                "WEATHER_ELEVATION_GAP",
                AnalysisCategory.DATA_QUALITY,
                "Prévision située nettement sous le point haut du GPX",
                "L’altitude du modèle météo au départ est inférieure au point haut de la trace ; température, vent et précipitations peuvent différer en altitude.",
                "Consulte une prévision montagne couvrant les altitudes du parcours et les conditions de crête.",
                "weatherModelElevationGapMeters",
                "Écart d’altitude",
                track.maximumElevationMeters()
                        - weather.modelElevationMeters(),
                "m",
                MODEL_GAP_CAUTION_METERS,
                MODEL_GAP_STRONG_METERS);
    }

    private void analyzePrecipitation(
            WeatherSummary weather,
            List<AnalysisFinding> findings) {
        Double amount = weather.precipitationSumMillimeters();
        Integer probability = weather
                .maximumPrecipitationProbabilityPercent();
        boolean strong = atLeast(amount, PRECIPITATION_STRONG_MILLIMETERS)
                || atLeast(probability, PRECIPITATION_STRONG_PERCENT);
        boolean caution = atLeast(amount, PRECIPITATION_CAUTION_MILLIMETERS)
                || atLeast(probability, PRECIPITATION_CAUTION_PERCENT);

        if (!strong && !caution) {
            return;
        }

        List<AnalysisEvidence> evidence = new ArrayList<>();
        addPrecipitationEvidence(
                evidence,
                amount,
                "precipitationSumMillimeters",
                "Cumul de précipitations",
                "mm",
                PRECIPITATION_CAUTION_MILLIMETERS,
                PRECIPITATION_STRONG_MILLIMETERS);
        addPrecipitationEvidence(
                evidence,
                probability == null ? null : probability.doubleValue(),
                "maximumPrecipitationProbabilityPercent",
                "Probabilité maximale",
                "%",
                PRECIPITATION_CAUTION_PERCENT,
                PRECIPITATION_STRONG_PERCENT);

        findings.add(new AnalysisFinding(
                "PRECIPITATION_AT_START",
                AnalysisCategory.WEATHER,
                strong
                        ? AnalysisSeverity.STRONG_CAUTION
                        : AnalysisSeverity.CAUTION,
                "Précipitations à examiner au point de départ",
                "Probabilité et cumul sont agrégés au départ ; ils ne décrivent ni l’intensité exacte ni l’ensemble du parcours.",
                "Consulte le bulletin local, les alertes et l’état des passages sensibles à l’eau avant de décider.",
                evidence));
    }

    private void addPrecipitationEvidence(
            List<AnalysisEvidence> evidence,
            Double value,
            String metric,
            String label,
            String unit,
            double cautionThreshold,
            double strongThreshold) {
        if (value == null || value < cautionThreshold) {
            return;
        }

        evidence.add(evidence(
                metric,
                label,
                value,
                unit,
                EvidenceComparison.GREATER_OR_EQUAL,
                value >= strongThreshold
                        ? strongThreshold
                        : cautionThreshold));
    }

    private AnalysisFinding unavailableFinding(
            WeatherStatus status) {
        return switch (status) {
            case NOT_REQUESTED -> new AnalysisFinding(
                    "WEATHER_NOT_REQUESTED",
                    AnalysisCategory.WEATHER,
                    AnalysisSeverity.NOTICE,
                    "Météo ponctuelle non demandée",
                    "Aucune coordonnée n’a été transmise au fournisseur ; les conditions restent donc à vérifier ailleurs.",
                    "Consulte une source météo officielle adaptée au secteur avant de décider.",
                    List.of());
            case OUTSIDE_FORECAST_HORIZON -> new AnalysisFinding(
                    "WEATHER_OUTSIDE_HORIZON",
                    AnalysisCategory.WEATHER,
                    AnalysisSeverity.NOTICE,
                    "Prévision pas encore disponible",
                    "La date planifiée est au-delà de l’horizon du fournisseur.",
                    "Actualise le contexte à l’approche du départ et consulte les sources officielles.",
                    List.of());
            case UNAVAILABLE -> new AnalysisFinding(
                    "WEATHER_UNAVAILABLE",
                    AnalysisCategory.WEATHER,
                    AnalysisSeverity.CAUTION,
                    "Prévision météo indisponible",
                    "Le fournisseur n’a pas renvoyé de donnée exploitable ; le moteur s’abstient d’interpréter les conditions.",
                    "Réessaie puis consulte une source officielle avant de décider.",
                    List.of());
            case AVAILABLE -> throw new IllegalArgumentException(
                    "Available weather is not an unavailable state.");
        };
    }
}
