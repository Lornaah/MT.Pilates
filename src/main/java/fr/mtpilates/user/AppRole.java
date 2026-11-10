package fr.mtpilates.user;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/** Rôles de l'enum `app_role` d'origine. ADMIN et USER sont historiques et n'accordent aucune capacité. */
public enum AppRole {
    OWNER, ADMIN, COACH, USER, SETTER, MEMBER, EXTERNAL_MEMBER,
    MANAGER_COACH, MANAGER_SETTER, MANAGER_CLOSER, CLOSER;

    /** Valeur stockée en base (minuscules, comme l'enum Postgres d'origine). */
    public String code() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Optional<AppRole> fromCode(final String code) {
        return Arrays.stream(values()).filter(role -> role.code().equals(code)).findFirst();
    }
}
