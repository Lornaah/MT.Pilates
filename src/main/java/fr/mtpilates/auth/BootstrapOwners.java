package fr.mtpilates.auth;

import fr.mtpilates.user.AppRole;
import fr.mtpilates.user.UserAccountRepository;
import fr.mtpilates.user.UserRole;
import fr.mtpilates.user.UserRoleRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Au démarrage, donne le rôle owner aux comptes déjà existants dont l'email est déclaré
 * dans la configuration (cas d'un owner ajouté après son inscription).
 */
@Component
public class BootstrapOwners implements ApplicationRunner {

    private final AuthProperties properties;
    private final UserAccountRepository accounts;
    private final UserRoleRepository roles;

    public BootstrapOwners(final AuthProperties properties, final UserAccountRepository accounts,
                           final UserRoleRepository roles) {
        this.properties = properties;
        this.accounts = accounts;
        this.roles = roles;
    }

    @Override
    @Transactional
    public void run(final ApplicationArguments args) {
        properties.ownerEmails().forEach(email -> accounts.findByEmail(email.trim())
                .filter(account -> !roles.existsByUserIdAndRole(account.getId(), AppRole.OWNER))
                .ifPresent(account -> roles.save(new UserRole(account.getId(), AppRole.OWNER))));
    }
}
