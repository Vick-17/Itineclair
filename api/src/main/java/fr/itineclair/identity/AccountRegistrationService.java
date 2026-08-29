package fr.itineclair.identity;

import java.util.Locale;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;

@Service
public class AccountRegistrationService {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountRegistrationService(
            UserAccountRepository userAccountRepository,
            PasswordEncoder passwordEncoder) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public RegisteredAccount register(String email, String rawPassword) {
        String normalizedEmail = normalizeEmail(email);

        if (userAccountRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyUsedException();
        }

        String passwordHash = passwordEncoder.encode(rawPassword);
        UserAccount account = UserAccount.create(normalizedEmail, passwordHash);

        try {
            UserAccount savedAccount = userAccountRepository.saveAndFlush(account);
            return RegisteredAccount.from(savedAccount);
        } catch (DataIntegrityViolationException exception) {
            if (isEmailUniqueConstraintViolation(exception)) {
                throw new EmailAlreadyUsedException(exception);
            }
            throw exception;
        }
    }

    private boolean isEmailUniqueConstraintViolation(Throwable exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof ConstraintViolationException constraintViolation) {
                return "uk_user_accounts_email".equalsIgnoreCase(
                        constraintViolation.getConstraintName());
            }
            cause = cause.getCause();
        }
        return false;
    }

    private String normalizeEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }
}
