package fr.itineclair.privacy;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface AccountDataStore {

    Optional<AccountExportSnapshot> loadSnapshot(UUID accountId);

    List<AccountExportSnapshot.TrackPoint> loadTrackPoints(
            UUID accountId,
            UUID trackId);

    int deleteAccount(UUID accountId);
}
