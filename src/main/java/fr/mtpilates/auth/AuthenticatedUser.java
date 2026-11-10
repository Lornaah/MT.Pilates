package fr.mtpilates.auth;

import java.util.List;
import java.util.UUID;
import org.springframework.security.core.userdetails.User;

/**
 * Principal stocké en session. Il ne porte que l'identité : les rôles sont relus
 * à chaque requête pour qu'un retrait de rôle s'applique immédiatement, comme l'annonce l'écran Équipe.
 */
public class AuthenticatedUser extends User {

    private final UUID id;

    public AuthenticatedUser(final UUID id, final String email, final String passwordHash) {
        super(email, passwordHash, List.of());
        this.id = id;
    }

    public UUID getId() {
        return id;
    }
}
