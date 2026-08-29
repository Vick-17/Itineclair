package fr.itineclair.identity;

import java.sql.SQLException;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AccountRegistrationServiceTest {

    private static final String RAW_PASSWORD = "une phrase de passe uniquement pour le test";

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AccountRegistrationService accountRegistrationService;

    @BeforeEach
    void setUp() {
        accountRegistrationService = new AccountRegistrationService(
                userAccountRepository,
                passwordEncoder);
    }

    @Test
    void registersAnAccountWithNormalizedEmailAndHashedPassword() {
        given(userAccountRepository.existsByEmail("victor@example.test"))
                .willReturn(false);

        given(passwordEncoder.encode(RAW_PASSWORD))
                .willReturn("{argon2id}$argon2id$hash-de-test");

        given(userAccountRepository.saveAndFlush(any(UserAccount.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        RegisteredAccount result = accountRegistrationService.register(
                "  Victor@Example.Test  ",
                RAW_PASSWORD);

        ArgumentCaptor<UserAccount> accountCaptor = ArgumentCaptor.forClass(UserAccount.class);

        verify(userAccountRepository)
                .saveAndFlush(accountCaptor.capture());

        UserAccount savedAccount = accountCaptor.getValue();

        assertThat(result.id()).isNotNull();
        assertThat(result.email()).isEqualTo("victor@example.test");
        assertThat(result.createdAt()).isNotNull();

        assertThat(savedAccount.email())
                .isEqualTo("victor@example.test");

        assertThat(savedAccount.passwordHash())
                .isEqualTo("{argon2id}$argon2id$hash-de-test")
                .doesNotContain(RAW_PASSWORD);

        assertThat(savedAccount.role()).isEqualTo(AccountRole.USER);
    }

    @Test
    void rejectsAnEmailThatAlreadyExists() {
        given(userAccountRepository.existsByEmail("victor@example.test"))
                .willReturn(true);

        assertThatThrownBy(() -> accountRegistrationService.register(
                "victor@example.test",
                RAW_PASSWORD))
                .isInstanceOf(EmailAlreadyUsedException.class);

        verify(passwordEncoder, never()).encode(anyString());
        verify(userAccountRepository, never())
                .saveAndFlush(any(UserAccount.class));
    }

    @Test
    void convertsConcurrentDuplicateIntoDomainError() {
        given(userAccountRepository.existsByEmail("victor@example.test"))
                .willReturn(false);

        given(passwordEncoder.encode(RAW_PASSWORD))
                .willReturn("{argon2id}$argon2id$hash-de-test");

        given(userAccountRepository.saveAndFlush(any(UserAccount.class)))
                .willThrow(new DataIntegrityViolationException(
                        "Duplicate email",
                        new ConstraintViolationException(
                                "Duplicate email",
                                new SQLException(),
                                "insert into user_accounts",
                                "uk_user_accounts_email")));

        assertThatThrownBy(() -> accountRegistrationService.register(
                "victor@example.test",
                RAW_PASSWORD))
                .isInstanceOf(EmailAlreadyUsedException.class);
    }

    @Test
    void preservesUnrelatedDatabaseConstraintViolation() {
        given(userAccountRepository.existsByEmail("victor@example.test"))
                .willReturn(false);

        given(passwordEncoder.encode(RAW_PASSWORD))
                .willReturn("{argon2id}$argon2id$hash-de-test");

        DataIntegrityViolationException databaseError = new DataIntegrityViolationException(
                "Invalid role",
                new ConstraintViolationException(
                        "Invalid role",
                        new SQLException(),
                        "insert into user_accounts",
                        "ck_user_accounts_role"));

        given(userAccountRepository.saveAndFlush(any(UserAccount.class)))
                .willThrow(databaseError);

        assertThatThrownBy(() -> accountRegistrationService.register(
                "victor@example.test",
                RAW_PASSWORD))
                .isSameAs(databaseError);
    }
}
