package fr.itineclair.identity;

import java.io.Serial;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public final class AccountPrincipal
        implements UserDetails, CredentialsContainer {

    @Serial
    private static final long serialVersionUID = 1L;

    private final UUID id;
    private final String email;
    private final AccountRole role;
    private String passwordHash;

    private AccountPrincipal(
            UUID id,
            String email,
            AccountRole role,
            String passwordHash) {
        this.id = id;
        this.email = email;
        this.role = role;
        this.passwordHash = passwordHash;
    }

    static AccountPrincipal from(UserAccount account) {
        return new AccountPrincipal(
                account.id(),
                account.email(),
                account.role(),
                account.passwordHash());
    }

    public UUID id() {
        return id;
    }

    public String email() {
        return email;
    }

    public AccountRole role() {
        return role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(
                new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public void eraseCredentials() {
        passwordHash = null;
    }

    @Override
    public boolean equals(Object other) {
        return this == other
                || (other instanceof AccountPrincipal principal
                && id.equals(principal.id));
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
