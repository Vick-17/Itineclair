package fr.itineclair.privacy;

import java.time.Instant;
import java.util.List;

public record AccountDataExportManifest(
        String format,
        int schemaVersion,
        Instant exportedAt,
        AccountExportSnapshot.Account account,
        List<AccountExportSnapshot.Track> tracks,
        List<String> notes) {

    private static final String FORMAT = "itineclair-account-export";
    private static final int SCHEMA_VERSION = 1;

    public AccountDataExportManifest {
        tracks = List.copyOf(tracks);
        notes = List.copyOf(notes);
    }

    static AccountDataExportManifest from(
            PreparedAccountExport preparedExport) {
        return new AccountDataExportManifest(
                FORMAT,
                SCHEMA_VERSION,
                preparedExport.exportedAt(),
                preparedExport.snapshot().account(),
                preparedExport.snapshot().tracks(),
                List.of(
                        "Les fichiers GPX sont reconstruits à partir "
                                + "des points acceptés lors de l’import.",
                        "Les extensions et métadonnées GPX non "
                                + "conservées à l’import ne peuvent pas "
                                + "être restituées.",
                        "Les analyses calculées à la demande ne sont "
                                + "pas persistées et ne figurent pas "
                                + "dans cette archive."));
    }
}
