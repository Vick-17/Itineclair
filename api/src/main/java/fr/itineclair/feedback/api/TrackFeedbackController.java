package fr.itineclair.feedback.api;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import fr.itineclair.feedback.TrackFeedbackService;
import fr.itineclair.identity.AccountPrincipal;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/tracks/{trackId}/feedback")
public class TrackFeedbackController {

    private final TrackFeedbackService trackFeedbackService;

    public TrackFeedbackController(TrackFeedbackService trackFeedbackService) {
        this.trackFeedbackService = trackFeedbackService;
    }

    @GetMapping
    public TrackFeedbackResponse get(
            @AuthenticationPrincipal AccountPrincipal principal,
            @PathVariable UUID trackId) {
        return trackFeedbackService.get(principal.id(), trackId)
                .map(TrackFeedbackResponse::from)
                .orElseGet(() -> TrackFeedbackResponse.notRecorded(trackId));
    }

    @PutMapping
    public TrackFeedbackResponse save(
            @AuthenticationPrincipal AccountPrincipal principal,
            @PathVariable UUID trackId,
            @Valid @RequestBody SaveTrackFeedbackRequest request) {
        return TrackFeedbackResponse.from(trackFeedbackService.save(
                principal.id(), trackId, request.toCommand()));
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal AccountPrincipal principal,
            @PathVariable UUID trackId) {
        trackFeedbackService.delete(principal.id(), trackId);
    }
}
