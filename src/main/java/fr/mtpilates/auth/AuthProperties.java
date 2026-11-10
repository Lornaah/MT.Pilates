package fr.mtpilates.auth;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * @param ownerEmails          emails qui reçoivent le rôle owner (variable d'environnement, jamais dans le code)
 * @param confirmationValidity durée de validité du lien de confirmation
 * @param mailFrom             expéditeur des emails
 */
@ConfigurationProperties(prefix = "app.auth")
public record AuthProperties(
        @DefaultValue("") List<String> ownerEmails,
        @DefaultValue("24h") Duration confirmationValidity,
        @DefaultValue("MT Pilates <no-reply@localhost>") String mailFrom) {

    public boolean isOwnerEmail(final String email) {
        return ownerEmails.stream()
                .map(e -> e.trim().toLowerCase(Locale.ROOT))
                .anyMatch(e -> !e.isEmpty() && e.equals(email.trim().toLowerCase(Locale.ROOT)));
    }
}
