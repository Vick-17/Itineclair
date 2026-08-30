package fr.itineclair.outdoor.api;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fr.itineclair.identity.AccountPrincipal;
import fr.itineclair.outdoor.OutdoorContextService;
import fr.itineclair.outdoor.OutdoorPlanCommand;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/tracks/{trackId}/outdoor-context")
public class OutdoorContextController {

    private final OutdoorContextService outdoorContextService;

    public OutdoorContextController(
            OutdoorContextService outdoorContextService) {
        this.outdoorContextService = outdoorContextService;
    }

    @GetMapping
    public OutdoorContextResponse get(
            @AuthenticationPrincipal AccountPrincipal principal,
            @PathVariable UUID trackId) {
        return outdoorContextService
                .getContext(principal.id(), trackId)
                .map(OutdoorContextResponse::from)
                .orElseGet(OutdoorContextResponse::notPlanned);
    }

    @PutMapping
    public OutdoorContextResponse save(
            @AuthenticationPrincipal AccountPrincipal principal,
            @PathVariable UUID trackId,
            @Valid @RequestBody SaveOutdoorContextRequest request) {
        OutdoorPlanCommand command = new OutdoorPlanCommand(
                request.plannedStartLocal(),
                request.plannedDurationMinutes(),
                request.timeZone(),
                request.shareStartPointWithWeatherProvider());

        return OutdoorContextResponse.from(
                outdoorContextService.saveContext(
                        principal.id(),
                        trackId,
                        command));
    }
}
