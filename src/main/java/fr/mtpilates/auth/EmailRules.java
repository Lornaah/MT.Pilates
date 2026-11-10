package fr.mtpilates.auth;

import java.util.Optional;
import java.util.regex.Pattern;

/** Règles d'email du schéma zod de l'original (emailSchema). */
public final class EmailRules {

    public static final String REQUIRED = "Email requis";
    public static final String INVALID = "Email invalide";
    public static final String TOO_LONG = "Email trop long";
    public static final int MAX_LENGTH = 255;

    /** Même expression que zod : points et « + » acceptés dans la partie locale, TLD obligatoire. */
    public static final String REGEX =
            "^(?!\\.)(?!.*\\.\\.)([A-Za-z0-9_'+\\-.]*)[A-Za-z0-9_+-]@([A-Za-z0-9][A-Za-z0-9\\-]*\\.)+[A-Za-z]{2,}$";
    private static final Pattern PATTERN = Pattern.compile(REGEX);

    private EmailRules() {
    }

    /** Première erreur, dans l'ordre de zod (requis, format, longueur). */
    public static Optional<String> firstError(final String normalizedEmail) {
        if (normalizedEmail.isEmpty()) {
            return Optional.of(REQUIRED);
        }
        if (!PATTERN.matcher(normalizedEmail).matches()) {
            return Optional.of(INVALID);
        }
        return normalizedEmail.length() > MAX_LENGTH ? Optional.of(TOO_LONG) : Optional.empty();
    }
}
