package fr.mtpilates.auth;

import fr.mtpilates.user.UserAccountRepository;
import java.util.Locale;
import java.util.UUID;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountUserDetailsService implements UserDetailsService {

    private final UserAccountRepository accounts;

    public AccountUserDetailsService(final UserAccountRepository accounts) {
        this.accounts = accounts;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(final String email) {
        return accounts.findByEmail(normalize(email))
                .map(account -> new AuthenticatedUser(account.getId(), account.getEmail(), account.getPasswordHash()))
                .orElseThrow(() -> new UsernameNotFoundException("Compte inconnu"));
    }

    @Transactional(readOnly = true)
    public UserDetails loadUserById(final UUID id) {
        return accounts.findById(id)
                .map(account -> new AuthenticatedUser(account.getId(), account.getEmail(), account.getPasswordHash()))
                .orElseThrow(() -> new UsernameNotFoundException("Compte inconnu"));
    }

    static String normalize(final String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
