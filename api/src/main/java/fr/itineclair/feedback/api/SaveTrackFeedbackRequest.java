package fr.itineclair.feedback.api;

import java.util.Set;

import fr.itineclair.feedback.ConditionsComparison;
import fr.itineclair.feedback.FeedbackIssue;
import fr.itineclair.feedback.FeedbackOutcome;
import fr.itineclair.feedback.TrackFeedbackCommand;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SaveTrackFeedbackRequest(
        @NotNull(message = "Le résultat de la sortie est obligatoire.")
        FeedbackOutcome outcome,
        @Min(value = 1, message = "La durée réelle doit être d’au moins 1 minute.")
        @Max(value = 1440, message = "La durée réelle ne peut pas dépasser 24 heures.")
        Integer actualDurationMinutes,
        @Min(value = 1, message = "Le ressenti doit être compris entre 1 et 5.")
        @Max(value = 5, message = "Le ressenti doit être compris entre 1 et 5.")
        Integer perceivedEffort,
        @NotNull(message = "La comparaison des conditions est obligatoire.")
        ConditionsComparison conditionsComparison,
        @NotNull(message = "La liste des difficultés observées est obligatoire.")
        @Size(max = 5, message = "La liste des difficultés observées est invalide.")
        Set<@NotNull(message = "Une difficulté observée est invalide.") FeedbackIssue> observedIssues) {

    TrackFeedbackCommand toCommand() {
        return new TrackFeedbackCommand(
                outcome,
                actualDurationMinutes,
                perceivedEffort,
                conditionsComparison,
                observedIssues);
    }
}
