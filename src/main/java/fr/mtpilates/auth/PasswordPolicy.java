package fr.mtpilates.auth;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Politique de mot de passe de l'original (password.ts) : 24 à 72 caractères,
 * minuscule, majuscule, chiffre et caractère non alphanumérique. Le plafond de 72
 * vient de BCrypt, qui ignore les octets suivants.
 */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 24;
    public static final int MAX_LENGTH = 72;

    public static final String TOO_SHORT = MIN_LENGTH + " caractères minimum";
    public static final String TOO_LONG = MAX_LENGTH + " caractères maximum";
    public static final String NO_LOWER = "Au moins une lettre minuscule (a-z)";
    public static final String NO_UPPER = "Au moins une lettre majuscule (A-Z)";
    public static final String NO_DIGIT = "Au moins un chiffre (0-9)";
    public static final String NO_SPECIAL = "Au moins un caractère spécial (! @ # $ % …)";

    private static final Pattern LOWER = Pattern.compile("[a-z]");
    private static final Pattern UPPER = Pattern.compile("[A-Z]");
    private static final Pattern DIGIT = Pattern.compile("\\d");
    private static final Pattern SPECIAL = Pattern.compile("[^A-Za-z0-9]");

    private PasswordPolicy() {
    }

    /** Messages des règles non respectées, dans l'ordre d'affichage de l'original. */
    public static List<String> violations(final String password) {
        final String value = password == null ? "" : password;
        final List<String> messages = new ArrayList<>();
        if (value.length() < MIN_LENGTH) {
            messages.add(TOO_SHORT);
        }
        if (value.length() > MAX_LENGTH) {
            messages.add(TOO_LONG);
        }
        addUnless(messages, LOWER.matcher(value).find(), NO_LOWER);
        addUnless(messages, UPPER.matcher(value).find(), NO_UPPER);
        addUnless(messages, DIGIT.matcher(value).find(), NO_DIGIT);
        addUnless(messages, SPECIAL.matcher(value).find(), NO_SPECIAL);
        return messages;
    }

    private static void addUnless(final List<String> messages, final boolean ok, final String message) {
        if (!ok) {
            messages.add(message);
        }
    }
}
