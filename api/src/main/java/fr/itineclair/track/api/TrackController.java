package fr.itineclair.track.api;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import fr.itineclair.identity.AccountPrincipal;
import fr.itineclair.track.TrackImportService;
import fr.itineclair.track.TrackSummary;

@RestController
@RequestMapping("/tracks")
public class TrackController {

    private final TrackImportService trackImportService;

    public TrackController(
            TrackImportService trackImportService) {
        this.trackImportService = trackImportService;
    }

    @GetMapping
    public List<TrackResponse> list(
            @AuthenticationPrincipal AccountPrincipal principal) {
        return trackImportService
                .listTracks(principal.id())
                .stream()
                .map(TrackResponse::from)
                .toList();
    }

    @GetMapping("/{trackId}")
    public TrackResponse get(
            @AuthenticationPrincipal AccountPrincipal principal,
            @PathVariable UUID trackId) {
        TrackSummary track = trackImportService.getTrack(
                principal.id(),
                trackId);

        return TrackResponse.from(track);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TrackResponse> importTrack(
            @AuthenticationPrincipal AccountPrincipal principal,
            @RequestParam("file") MultipartFile file) {
        TrackSummary imported = trackImportService.importGpx(
                principal.id(),
                file);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(TrackResponse.from(imported));
    }
}
