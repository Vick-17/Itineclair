package fr.itineclair.analysis.api;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fr.itineclair.analysis.TrackAnalysisService;
import fr.itineclair.identity.AccountPrincipal;

@RestController
@RequestMapping("/tracks/{trackId}/analysis")
public class TrackAnalysisController {

    private final TrackAnalysisService trackAnalysisService;

    public TrackAnalysisController(
            TrackAnalysisService trackAnalysisService) {
        this.trackAnalysisService = trackAnalysisService;
    }

    @GetMapping
    public TrackAnalysisResponse get(
            @AuthenticationPrincipal AccountPrincipal principal,
            @PathVariable UUID trackId) {
        return TrackAnalysisResponse.from(
                trackAnalysisService.analyze(
                        principal.id(),
                        trackId));
    }
}
