package fr.itineclair.analysis;

import java.time.Instant;
import java.util.List;

import fr.itineclair.outdoor.OutdoorContextView;
import fr.itineclair.track.TrackSummary;

import static fr.itineclair.analysis.AnalysisRuleSupport.addHighThresholdFinding;
import static fr.itineclair.analysis.AnalysisRuleSupport.evidence;

final class OutdoorPlanRuleEvaluator {

    private static final double DURATION_CAUTION_HOURS = 8.0;
    private static final double DURATION_STRONG_HOURS = 12.0;
    private static final double DARKNESS_STRONG_MINUTES = 120.0;

    private final WeatherRuleEvaluator weatherRules =
            new WeatherRuleEvaluator();

    void evaluate(
            TrackSummary track,
            OutdoorContextView context,
            Instant generatedAt,
            List<AnalysisFinding> findings) {
        addHighThresholdFinding(
                findings,
                "LONG_PLANNED_DURATION",
                AnalysisCategory.PHYSICAL_LOAD,
                "Fenêtre de sortie longue",
                "La durée planifiée augmente l’exposition aux changements de conditions et à la fatigue.",
                "Prévois pauses, eau, alimentation, marge horaire et itinéraire de repli adaptés au groupe.",
                "plannedDurationHours",
                "Durée prévue",
                context.plannedDurationMinutes() / 60.0,
                "h",
                DURATION_CAUTION_HOURS,
                DURATION_STRONG_HOURS);

        analyzeDarkness(context, findings);
        weatherRules.evaluate(
                track,
                context.weather(),
                generatedAt,
                findings);
    }

    private void analyzeDarkness(
            OutdoorContextView context,
            List<AnalysisFinding> findings) {
        double darknessMinutes = context.daylight()
                .expectedDarknessMinutes();
        if (darknessMinutes <= 0.0) {
            return;
        }

        AnalysisSeverity severity = darknessMinutes
                >= DARKNESS_STRONG_MINUTES
                ? AnalysisSeverity.STRONG_CAUTION
                : AnalysisSeverity.CAUTION;
        findings.add(new AnalysisFinding(
                "EXPECTED_DARKNESS",
                AnalysisCategory.LIGHT,
                severity,
                "Une partie de la sortie est prévue hors crépuscule civil",
                "Le calcul astronomique prévoit de l’obscurité pendant la fenêtre choisie ; relief et forêt peuvent réduire la lumière plus tôt.",
                "Prévois une lampe testée, une marge et vérifie que le groupe accepte une progression de nuit.",
                List.of(evidence(
                        "expectedDarknessMinutes",
                        "Obscurité calculée",
                        darknessMinutes,
                        "min",
                        severity == AnalysisSeverity.STRONG_CAUTION
                                ? EvidenceComparison.GREATER_OR_EQUAL
                                : EvidenceComparison.GREATER_THAN,
                        severity == AnalysisSeverity.STRONG_CAUTION
                                ? DARKNESS_STRONG_MINUTES
                                : 0.0))));
    }
}
