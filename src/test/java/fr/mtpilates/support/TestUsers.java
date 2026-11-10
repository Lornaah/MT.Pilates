package fr.mtpilates.support;

import fr.mtpilates.auth.AuthenticatedUser;
import fr.mtpilates.user.AppRole;
import java.util.Arrays;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

/** Création de comptes de test et connexion MockMvc en leur nom. */
public class TestUsers {

    public static final String PASSWORD = "MotDePasseDeTest-Tres-Long-2026!";

    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;

    public TestUsers(final JdbcTemplate jdbc, final PasswordEncoder encoder) {
        this.jdbc = jdbc;
        this.encoder = encoder;
    }

    /** Crée un compte (email unique dérivé du préfixe) avec son profil et ses rôles. */
    public Account create(final String prefix, final AppRole... roles) {
        final UUID id = UUID.randomUUID();
        final String email = prefix + "-" + id.toString().substring(0, 8) + "@example.com";
        final String hash = encoder.encode(PASSWORD);
        jdbc.update("INSERT INTO users (id, email, password_hash) VALUES (?, ?, ?)", id, email, hash);
        jdbc.update("INSERT INTO profiles (id, email, first_name, last_name, onboarding_completed_at) VALUES (?, ?, ?, ?, now())",
                id, email, capitalize(prefix), "Test");
        Arrays.stream(roles).forEach(role ->
                jdbc.update("INSERT INTO user_roles (user_id, role) VALUES (?, ?)", id, role.code()));
        return new Account(id, email, hash);
    }

    private static String capitalize(final String value) {
        return value.substring(0, 1).toUpperCase() + value.substring(1);
    }

    public record Account(UUID id, String email, String passwordHash) {

        /** Requête MockMvc authentifiée au nom de ce compte. */
        public RequestPostProcessor login() {
            return user(new AuthenticatedUser(id, email, passwordHash));
        }
    }
}
