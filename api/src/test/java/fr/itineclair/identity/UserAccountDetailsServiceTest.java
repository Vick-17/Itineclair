package fr.itineclair.identity;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserAccountDetailsServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    private UserAccountDetailsService userAccountDetailsService;

    @BeforeEach
    void setUp() {
        userAccountDetailsService =
                new UserAccountDetailsService(userAccountRepository);
    }

    @Test
    void loadsAccountWithNormalizedEmail() {
        UserAccount account = UserAccount.create(
                "victor@example.test",
                "{argon2id}$argon2id$hash-de-test");
        given(userAccountRepository.findByEmail("victor@example.test"))
                .willReturn(Optional.of(account));

        AccountPrincipal principal = (AccountPrincipal) userAccountDetailsService
                .loadUserByUsername("  Victor@Example.Test  ");

        assertThat(principal.id()).isEqualTo(account.id());
        assertThat(principal.email()).isEqualTo("victor@example.test");
        assertThat(principal.role()).isEqualTo(AccountRole.USER);
        assertThat(principal.getPassword())
                .isEqualTo("{argon2id}$argon2id$hash-de-test");
        assertThat(principal.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_USER");
        verify(userAccountRepository).findByEmail("victor@example.test");
    }

    @Test
    void rejectsUnknownAccountWithoutIncludingEmailInError() {
        given(userAccountRepository.findByEmail("unknown@example.test"))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> userAccountDetailsService
                .loadUserByUsername("unknown@example.test"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("Account not found.")
                .hasMessageNotContaining("unknown@example.test");
    }

    @Test
    void erasesPasswordHashBeforePrincipalIsStoredInSession() {
        AccountPrincipal principal = AccountPrincipal.from(UserAccount.create(
                "victor@example.test",
                "{argon2id}$argon2id$hash-de-test"));
        principal.eraseCredentials();
        assertThat(principal.getPassword()).isNull();
    }
}
