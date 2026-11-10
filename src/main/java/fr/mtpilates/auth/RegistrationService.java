package fr.mtpilates.auth;

import fr.mtpilates.user.AppRole;
import fr.mtpilates.user.Profile;
import fr.mtpilates.user.ProfileRepository;
import fr.mtpilates.user.UserAccount;
import fr.mtpilates.user.UserAccountRepository;
import fr.mtpilates.user.UserRole;
import fr.mtpilates.user.UserRoleRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Inscription en deux temps (décision Q3) : email + mot de passe, puis le compte est
 * créé au clic sur le lien reçu par email. Remplace signUp / resend de Supabase.
 */
@Service
public class RegistrationService {

    private final PendingRegistrationRepository pendings;
    private final UserAccountRepository accounts;
    private final ProfileRepository profiles;
    private final UserRoleRepository roles;
    private final PasswordEncoder passwordEncoder;
    private final RegistrationMailer mailer;
    private final AuthProperties properties;
    private final Clock clock;

    public RegistrationService(final PendingRegistrationRepository pendings, final UserAccountRepository accounts,
                               final ProfileRepository profiles, final UserRoleRepository roles,
                               final PasswordEncoder passwordEncoder, final RegistrationMailer mailer,
                               final AuthProperties properties, final Clock clock) {
        this.pendings = pendings;
        this.accounts = accounts;
        this.profiles = profiles;
        this.roles = roles;
        this.passwordEncoder = passwordEncoder;
        this.mailer = mailer;
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * Enregistre (ou remplace) une inscription en attente et envoie le lien de confirmation.
     *
     * @param linkForToken construit l'URL absolue de confirmation à partir du jeton
     */
    @Transactional
    public void register(final String email, final String rawPassword, final Function<String, String> linkForToken) {
        if (accounts.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException();
        }
        final String token = ConfirmationTokens.newToken();
        final String passwordHash = passwordEncoder.encode(rawPassword);
        pendings.findByEmail(email).ifPresentOrElse(
                pending -> {
                    pending.replacePassword(passwordHash);
                    pending.renew(ConfirmationTokens.hash(token), expiry());
                },
                () -> pendings.save(new PendingRegistration(email, passwordHash, ConfirmationTokens.hash(token), expiry())));
        mailer.sendConfirmation(email, linkForToken.apply(token));
    }

    /** Renvoie un nouveau lien si une inscription est en attente ; ne révèle rien sinon. */
    @Transactional
    public void resend(final String email, final Function<String, String> linkForToken) {
        pendings.findByEmail(email).ifPresent(pending -> {
            final String token = ConfirmationTokens.newToken();
            pending.renew(ConfirmationTokens.hash(token), expiry());
            mailer.sendConfirmation(pending.getEmail(), linkForToken.apply(token));
        });
    }

    /** Crée le compte à partir d'un lien valide et renvoie l'identifiant du nouvel utilisateur. */
    @Transactional
    public Optional<UUID> confirm(final String token) {
        return pendings.findByTokenHash(ConfirmationTokens.hash(token))
                .filter(pending -> !pending.isExpired(clock.instant()))
                .filter(pending -> !accounts.existsByEmail(pending.getEmail()))
                .map(this::createAccount);
    }

    /** Vrai si l'email a une inscription non confirmée avec ce mot de passe (message dédié à la connexion). */
    @Transactional(readOnly = true)
    public boolean isAwaitingConfirmation(final String email, final String rawPassword) {
        return pendings.findByEmail(email)
                .filter(pending -> !pending.isExpired(clock.instant()))
                .filter(pending -> passwordEncoder.matches(rawPassword, pending.getPasswordHash()))
                .isPresent();
    }

    @Scheduled(cron = "0 17 3 * * *", zone = "Europe/Paris")
    @Transactional
    public void purgeExpired() {
        pendings.deleteExpired(clock.instant());
    }

    private UUID createAccount(final PendingRegistration pending) {
        final UUID id = UUID.randomUUID();
        accounts.save(new UserAccount(id, pending.getEmail(), pending.getPasswordHash()));
        profiles.save(new Profile(id, pending.getEmail()));
        roles.save(new UserRole(id, properties.isOwnerEmail(pending.getEmail()) ? AppRole.OWNER : AppRole.MEMBER));
        pendings.delete(pending);
        return id;
    }

    private Instant expiry() {
        return clock.instant().plus(properties.confirmationValidity());
    }
}
