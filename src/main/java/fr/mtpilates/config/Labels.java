package fr.mtpilates.config;

import java.util.Map;

/** Instantané de la configuration : `label(clé, défaut)` comme le hook useAppConfig de l'original. */
public record Labels(Map<String, String> values) {

    public Labels {
        values = Map.copyOf(values);
    }

    /** Valeur configurée, ou le libellé par défaut si la clé est absente ou vide (« Réinitialiser » vide les valeurs). */
    public String label(final String key, final String fallback) {
        final String value = values.get(key);
        return value == null || value.isEmpty() ? fallback : value;
    }
}
