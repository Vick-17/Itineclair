package fr.itineclair.identity;

import java.util.Locale;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserAccountDetailsService implements UserDetailsService {

    private final UserAccountRepository userAccountRepository;

    public UserAccountDetailsService(
            UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        String normalizedEmail = normalizeEmail(email);

        return userAccountRepository
                .findByEmail(normalizedEmail)
                .map(AccountPrincipal::from)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "Account not found."));
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            throw new UsernameNotFoundException(
                    "Account not found.");
        }

        return email
                .strip()
                .toLowerCase(Locale.ROOT);
    }
}
