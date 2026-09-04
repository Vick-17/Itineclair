package fr.itineclair.profile;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HikerProfileService {

    private final HikerProfileRepository profileRepository;
    private final Clock clock;

    public HikerProfileService(
            HikerProfileRepository profileRepository,
            Clock clock) {
        this.profileRepository = profileRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Optional<HikerProfileView> get(UUID accountId) {
        Objects.requireNonNull(accountId, "accountId");

        return profileRepository.findById(accountId)
                .map(HikerProfileView::from);
    }

    @Transactional
    public HikerProfileView save(
            UUID accountId,
            HikerProfileCommand command) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(command, "command");
        validate(command);

        Instant now = clock.instant();

        HikerProfile profile = profileRepository.findById(accountId)
                .map(existingProfile -> {
                    existingProfile.update(command, now);
                    return existingProfile;
                })
                .orElseGet(() -> HikerProfile.create(
                        accountId,
                        command,
                        now));

        return HikerProfileView.from(
                profileRepository.saveAndFlush(profile));
    }

    @Transactional
    public void delete(UUID accountId) {
        Objects.requireNonNull(accountId, "accountId");
        profileRepository.deleteById(accountId);
    }

    private void validate(HikerProfileCommand command) {
        if (command.experienceLevel() == null) {
            throw new InvalidHikerProfileException(
                    "Le niveau de pratique auto-déclaré est obligatoire.");
        }

        validateRange(
                command.usualDurationMinutes(),
                15,
                1440,
                "La durée habituelle doit être comprise entre 15 minutes et 24 heures.");

        validateRange(
                command.usualDistanceMeters(),
                500,
                100000,
                "La distance habituelle doit être comprise entre 500 mètres et 100 kilomètres.");

        validateRange(
                command.usualElevationGainMeters(),
                0,
                10000,
                "Le dénivelé habituel doit être compris entre 0 et 10 000 mètres.");
    }

    private void validateRange(
            Integer value,
            int minimum,
            int maximum,
            String message) {
        if (value != null && (value < minimum || value > maximum)) {
            throw new InvalidHikerProfileException(message);
        }
    }
}
