package fr.itineclair.analysis;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import fr.itineclair.outdoor.OutdoorContextView;
import fr.itineclair.outdoor.WeatherStatus;
import fr.itineclair.outdoor.WeatherSummary;
import fr.itineclair.track.TrackSummary;

final class AnalysisChecklistFactory {

    List<AnalysisChecklistItem> create(
            TrackSummary track,
            Optional<OutdoorContextView> optionalContext) {
        List<AnalysisChecklistItem> items = new ArrayList<>();
        items.add(traceFacts(track));
        items.add(plannedWindow(optionalContext));
        items.add(weather(optionalContext));
        items.add(new AnalysisChecklistItem(
                "OFFICIAL_ALERTS_AND_CLOSURES",
                ChecklistStatus.TO_VERIFY,
                "Alertes officielles, fermetures et réglementation",
                "Non intégrées : consulter les autorités, gestionnaires et la Vigilance avant le départ."));
        items.add(new AnalysisChecklistItem(
                "TERRAIN_AND_EXPOSURE",
                ChecklistStatus.TO_VERIFY,
                "État du terrain, exposition et passages techniques",
                "Non déductibles du GPX : utiliser des sources locales récentes ou un professionnel."));
        items.add(new AnalysisChecklistItem(
                "GROUP_AND_EQUIPMENT",
                ChecklistStatus.TO_VERIFY,
                "Niveau du groupe, santé, matériel et plan de repli",
                "À évaluer pour chaque personne, en prenant le membre le moins expérimenté comme référence."));
        return List.copyOf(items);
    }

    private AnalysisChecklistItem traceFacts(
            TrackSummary track) {
        return new AnalysisChecklistItem(
                "TRACE_FACTS",
                track.factsAvailable()
                        ? ChecklistStatus.AVAILABLE
                        : ChecklistStatus.TO_VERIFY,
                "Mesures de la trace",
                track.factsAvailable()
                        ? "Distance et géométrie calculées depuis le GPX."
                        : "Mesures indisponibles.");
    }

    private AnalysisChecklistItem plannedWindow(
            Optional<OutdoorContextView> optionalContext) {
        return new AnalysisChecklistItem(
                "PLANNED_WINDOW_AND_LIGHT",
                optionalContext.isPresent()
                        ? ChecklistStatus.AVAILABLE
                        : ChecklistStatus.TO_VERIFY,
                "Horaire et lumière",
                optionalContext.isPresent()
                        ? "Fenêtre planifiée et lumière astronomique calculée."
                        : "Date, heure, durée et lumière à renseigner.");
    }

    private AnalysisChecklistItem weather(
            Optional<OutdoorContextView> optionalContext) {
        if (optionalContext.isEmpty()) {
            return new AnalysisChecklistItem(
                    "POINT_WEATHER",
                    ChecklistStatus.TO_VERIFY,
                    "Prévision météo",
                    "Aucune fenêtre planifiée ni prévision disponible.");
        }

        WeatherSummary weather = optionalContext.get().weather();
        if (weather.status() == WeatherStatus.AVAILABLE) {
            return new AnalysisChecklistItem(
                    "POINT_WEATHER",
                    ChecklistStatus.PARTIAL,
                    "Prévision météo",
                    "Prévision ponctuelle disponible au départ ; compléter avec bulletins montagne et alertes officielles.");
        }

        return new AnalysisChecklistItem(
                "POINT_WEATHER",
                ChecklistStatus.TO_VERIFY,
                "Prévision météo",
                "Prévision ponctuelle indisponible ou non demandée.");
    }
}
