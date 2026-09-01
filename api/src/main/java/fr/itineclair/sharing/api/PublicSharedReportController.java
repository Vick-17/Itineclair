package fr.itineclair.sharing.api;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import fr.itineclair.sharing.SharedTrackReport;
import fr.itineclair.sharing.TrackShareService;

@RestController
public class PublicSharedReportController {

    public static final String TOKEN_HEADER =
            "X-Itineclair-Share-Token";

    private final TrackShareService trackShareService;

    public PublicSharedReportController(
            TrackShareService trackShareService) {
        this.trackShareService = trackShareService;
    }

    @GetMapping("/shared-report")
    public ResponseEntity<SharedTrackReportResponse> get(
            @RequestHeader(
                    name = TOKEN_HEADER,
                    required = false)
            String token) {
        SharedTrackReport report = trackShareService
                .getPublicReport(token);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header("Referrer-Policy", "no-referrer")
                .header("X-Robots-Tag", "noindex, nofollow")
                .body(SharedTrackReportResponse.from(report));
    }
}
