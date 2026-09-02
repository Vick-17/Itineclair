package fr.itineclair.privacy;

import java.time.Instant;

public record PreparedAccountExport(
        String filename,
        Instant exportedAt,
        AccountExportSnapshot snapshot) {
}
