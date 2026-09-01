package fr.itineclair.sharing.api;

import java.util.UUID;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import fr.itineclair.identity.AccountPrincipal;
import fr.itineclair.sharing.CreatedTrackShare;
import fr.itineclair.sharing.SharedTrackReport;
import fr.itineclair.sharing.TrackShareService;
import fr.itineclair.sharing.TrackShareStatus;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/tracks/{trackId}/share")
public class TrackShareController {

    private final TrackShareService trackShareService;

    public TrackShareController(TrackShareService trackShareService) {
        this.trackShareService = trackShareService;
    }

    @GetMapping
    public ResponseEntity<TrackShareStatusResponse> status(
            @AuthenticationPrincipal AccountPrincipal principal,
            @PathVariable UUID trackId) {
        TrackShareStatus status = trackShareService.getStatus(
                principal.id(),
                trackId);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(TrackShareStatusResponse.from(status));
    }

    @GetMapping("/preview")
    public ResponseEntity<SharedTrackReportResponse> preview(
            @AuthenticationPrincipal AccountPrincipal principal,
            @PathVariable UUID trackId) {
        SharedTrackReport report = trackShareService.preview(
                principal.id(),
                trackId);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(SharedTrackReportResponse.from(report));
    }

    @PostMapping
    public ResponseEntity<CreatedTrackShareResponse> create(
            @AuthenticationPrincipal AccountPrincipal principal,
            @PathVariable UUID trackId,
            @Valid @RequestBody CreateTrackShareRequest request) {
        CreatedTrackShare share = trackShareService.createOrRotate(
                principal.id(),
                trackId,
                request.durationDays());

        return ResponseEntity.status(HttpStatus.CREATED)
                .cacheControl(CacheControl.noStore())
                .body(CreatedTrackShareResponse.from(share));
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(
            @AuthenticationPrincipal AccountPrincipal principal,
            @PathVariable UUID trackId) {
        trackShareService.revoke(principal.id(), trackId);
    }
}
