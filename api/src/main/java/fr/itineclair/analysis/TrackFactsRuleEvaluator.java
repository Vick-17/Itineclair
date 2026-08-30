package fr.itineclair.analysis;

import java.util.List;

import fr.itineclair.track.TrackSummary;

import static fr.itineclair.analysis.AnalysisRuleSupport.addHighThresholdFinding;
import static fr.itineclair.analysis.AnalysisRuleSupport.addOptionalHighThresholdFinding;
import static fr.itineclair.analysis.AnalysisRuleSupport.evidence;
import static fr.itineclair.analysis.AnalysisRuleSupport.maximum;

final class TrackFactsRuleEvaluator {

    private static final double DISTANCE_CAUTION_KILOMETERS = 20.0;
    private static final double DISTANCE_STRONG_KILOMETERS = 35.0;
    private static final double GAIN_CAUTION_METERS = 1_000.0;
    private static final double GAIN_STRONG_METERS = 1_600.0;
    private static final double ALTITUDE_CAUTION_METERS = 2_500.0;
    private static final double ALTITUDE_STRONG_METERS = 3_000.0;
    private static final double GRADE_CAUTION_PERCENT = 30.0;
    private static final double GRADE_STRONG_PERCENT = 50.0;

    void evaluate(
            TrackSummary track,
            List<AnalysisFinding> findings) {
        if (!track.factsAvailable()) {
            findings.add(new AnalysisFinding(
                    "FACTS_UNAVAILABLE",
                    AnalysisCategory.DATA_QUALITY,
                    AnalysisSeverity.CAUTION,
                    "Faits GPX indisponibles",
                    "La trace ne fournit pas encore les mesures nécessaires au moteur de règles.",
                    "Recharge la trace ou réimporte le fichier avant d’interpréter le rapport.",
                    List.of()));
            return;
        }

        analyzeElevationCoverage(track, findings);

        addHighThresholdFinding(
                findings,
                "DISTANCE_LOAD",
                AnalysisCategory.PHYSICAL_LOAD,
                "Distance importante à comparer au groupe",
                "La longueur brute augmente la charge et le temps d’exposition, sans décrire à elle seule la difficulté.",
                "Compare cette distance aux sorties récentes du membre le moins expérimenté et prévois une marge.",
                "distanceKilometers",
                "Distance",
                track.totalDistanceMeters() / 1_000.0,
                "km",
                DISTANCE_CAUTION_KILOMETERS,
                DISTANCE_STRONG_KILOMETERS);

        addOptionalHighThresholdFinding(
                findings,
                "ELEVATION_GAIN_LOAD",
                AnalysisCategory.PHYSICAL_LOAD,
                "Dénivelé positif important à comparer au groupe",
                "Le dénivelé positif brut est un indicateur de charge, mais ne tient pas compte du terrain ni du rythme.",
                "Compare ce dénivelé à l’expérience récente du groupe et conserve une marge de temps et d’énergie.",
                "elevationGainMeters",
                "Dénivelé positif",
                track.elevationGainMeters(),
                "m",
                GAIN_CAUTION_METERS,
                GAIN_STRONG_METERS);

        addOptionalHighThresholdFinding(
                findings,
                "HIGH_ALTITUDE",
                AnalysisCategory.ROUTE_CHARACTERISTICS,
                "Altitude élevée dans le GPX",
                "L’altitude maximale mérite une préparation spécifique ; le GPX ne permet pas d’évaluer l’acclimatation ni les conditions locales.",
                "Vérifie la météo montagne, les effets de l’altitude et l’expérience de chaque personne.",
                "maximumElevationMeters",
                "Altitude maximale",
                track.maximumElevationMeters(),
                "m",
                ALTITUDE_CAUTION_METERS,
                ALTITUDE_STRONG_METERS);

        addOptionalHighThresholdFinding(
                findings,
                "STEEP_GRADE",
                AnalysisCategory.ROUTE_CHARACTERISTICS,
                "Pente maximale marquée dans le GPX",
                "Cette valeur est calculée sur une fenêtre d’au moins 25 m ; elle peut refléter le relief ou du bruit GPS et ne décrit pas l’exposition.",
                "Contrôle le passage sur une carte, un topo récent et, si nécessaire, auprès d’un professionnel local.",
                "maximumAbsoluteGradePercent",
                "Pente absolue maximale",
                maximum(
                        track.maximumUphillGradePercent(),
                        track.maximumDownhillGradePercent()),
                "%",
                GRADE_CAUTION_PERCENT,
                GRADE_STRONG_PERCENT);
    }

    private void analyzeElevationCoverage(
            TrackSummary track,
            List<AnalysisFinding> findings) {
        if (track.elevationPointCount() >= track.pointCount()) {
            return;
        }

        double coveragePercent = track.pointCount() == 0
                ? 0.0
                : track.elevationPointCount() * 100.0
                        / track.pointCount();
        findings.add(new AnalysisFinding(
                "ELEVATION_INCOMPLETE",
                AnalysisCategory.DATA_QUALITY,
                AnalysisSeverity.CAUTION,
                "Altitude incomplète",
                "Les dénivelés, altitudes et pentes sont partiels car certains points n’ont pas d’altitude.",
                "Vérifie ces valeurs avec une autre source avant d’estimer l’effort ou le terrain.",
                List.of(evidence(
                        "elevationCoveragePercent",
                        "Points avec altitude",
                        coveragePercent,
                        "%",
                        EvidenceComparison.LESS_THAN,
                        100.0))));
    }
}
