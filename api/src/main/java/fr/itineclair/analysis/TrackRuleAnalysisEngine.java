package fr.itineclair.analysis;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import fr.itineclair.outdoor.OutdoorContextView;
import fr.itineclair.outdoor.WeatherSummary;
import fr.itineclair.track.TrackSummary;

/**
 * Applies the versioned MVP heuristics documented in
 * {@code docs/rule-analysis.md}. These thresholds are triage aids, not
 * official mountain standards and not a safety verdict.
 */
@Component
public class TrackRuleAnalysisEngine {

    private static final List<String> LIMITATIONS = List.of(
            "Aucune alerte officielle, fermeture ni bulletin avalanche n’est intégré.",
            "La météo décrit seulement le point de départ à l’altitude du modèle, pas l’ensemble du parcours.",
            "Les seuils du MVP ne sont pas personnalisés au groupe et attendent une relecture par des professionnels qualifiés.",
            "Le GPX ne décrit pas l’exposition, l’état du sentier ni les difficultés techniques réelles.");

    private final TrackFactsRuleEvaluator trackRules =
            new TrackFactsRuleEvaluator();
    private final OutdoorPlanRuleEvaluator outdoorRules =
            new OutdoorPlanRuleEvaluator();
    private final AnalysisChecklistFactory checklistFactory =
            new AnalysisChecklistFactory();

    public TrackAnalysis analyze(
            TrackSummary track,
            Optional<OutdoorContextView> optionalContext,
            Instant generatedAt) {
        List<AnalysisFinding> findings = new ArrayList<>();

        trackRules.evaluate(track, findings);
        optionalContext.ifPresentOrElse(
                context -> outdoorRules.evaluate(
                        track,
                        context,
                        generatedAt,
                        findings),
                () -> findings.add(noPlanFinding()));

        findings.sort(Comparator
                .comparingInt((AnalysisFinding finding) ->
                        finding.severity().priority())
                .reversed()
                .thenComparing(finding -> finding.category().ordinal())
                .thenComparing(AnalysisFinding::code));

        return new TrackAnalysis(
                TrackAnalysis.CURRENT_RULE_SET_VERSION,
                RuleReviewStatus.PROTOTYPE_AWAITING_EXPERT_REVIEW,
                generatedAt,
                sourceSnapshot(track, optionalContext),
                findings,
                checklistFactory.create(track, optionalContext),
                LIMITATIONS);
    }

    private AnalysisFinding noPlanFinding() {
        return new AnalysisFinding(
                "PLAN_NOT_CONFIGURED",
                AnalysisCategory.DATA_QUALITY,
                AnalysisSeverity.NOTICE,
                "Horaire et conditions non analysés",
                "Sans date, heure et durée, le moteur ne peut pas examiner la lumière ni une prévision météo.",
                "Planifie la sortie pour compléter les dimensions lumière et conditions.",
                List.of());
    }

    private AnalysisSourceSnapshot sourceSnapshot(
            TrackSummary track,
            Optional<OutdoorContextView> optionalContext) {
        return new AnalysisSourceSnapshot(
                track.factsVersion(),
                optionalContext.map(OutdoorContextView::updatedAt)
                        .orElse(null),
                optionalContext.map(OutdoorContextView::weather)
                        .map(WeatherSummary::checkedAt)
                        .orElse(null));
    }
}
