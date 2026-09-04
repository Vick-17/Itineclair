package fr.itineclair.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HikerProfileServiceTest {

    private static final UUID ACCOUNT_ID =
            UUID.fromString(
                    "936dd470-a45c-46fa-a0bd-94a76e4b836a");

    private static final Instant NOW =
            Instant.parse("2026-09-02T10:00:00Z");

    @Mock
    private HikerProfileRepository profileRepository;

    private HikerProfileService service;

    @BeforeEach
    void setUp() {
        service = new HikerProfileService(
                profileRepository,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void createsADeclaredProfileWithServerTimestamps() {
        given(profileRepository.findById(ACCOUNT_ID))
                .willReturn(Optional.empty());

        given(profileRepository.saveAndFlush(
                any(HikerProfile.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        HikerProfileView result =
                service.save(ACCOUNT_ID, command());

        assertThat(result.accountId()).isEqualTo(ACCOUNT_ID);
        assertThat(result.experienceLevel())
                .isEqualTo(ExperienceLevel.REGULAR);
        assertThat(result.usualDurationMinutes()).isEqualTo(360);
        assertThat(result.usualDistanceMeters()).isEqualTo(14_000);
        assertThat(result.usualElevationGainMeters()).isEqualTo(900);
        assertThat(result.createdAt()).isEqualTo(NOW);
        assertThat(result.updatedAt()).isEqualTo(NOW);
    }

    @Test
    void updatesAProfileWithoutChangingItsCreationDate() {
        Instant createdAt = NOW.minusSeconds(86_400);

        HikerProfile existing = HikerProfile.create(
                ACCOUNT_ID,
                new HikerProfileCommand(
                        ExperienceLevel.OCCASIONAL,
                        null,
                        null,
                        null),
                createdAt);

        given(profileRepository.findById(ACCOUNT_ID))
                .willReturn(Optional.of(existing));

        given(profileRepository.saveAndFlush(existing))
                .willReturn(existing);

        HikerProfileView result =
                service.save(ACCOUNT_ID, command());

        assertThat(result.createdAt()).isEqualTo(createdAt);
        assertThat(result.updatedAt()).isEqualTo(NOW);
        assertThat(result.experienceLevel())
                .isEqualTo(ExperienceLevel.REGULAR);
    }

    @Test
    void rejectsOutOfRangeValuesBeforeReadingTheDatabase() {
        HikerProfileCommand invalid =
                new HikerProfileCommand(
                        ExperienceLevel.REGULAR,
                        14,
                        null,
                        null);

        assertThatThrownBy(
                () -> service.save(ACCOUNT_ID, invalid))
                .isInstanceOf(
                        InvalidHikerProfileException.class)
                .hasMessageContaining("15 minutes");

        verifyNoInteractions(profileRepository);
    }

    @Test
    void deletesOnlyTheAuthenticatedAccountsProfile() {
        service.delete(ACCOUNT_ID);

        verify(profileRepository).deleteById(ACCOUNT_ID);
    }

    private HikerProfileCommand command() {
        return new HikerProfileCommand(
                ExperienceLevel.REGULAR,
                360,
                14_000,
                900);
    }
}
