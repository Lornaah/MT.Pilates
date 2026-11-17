package fr.mtpilates.config;

import fr.mtpilates.auth.CurrentUser;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Configuration de l'application. Règle d'origine : lecture pour tous, écriture par l'owner seulement. */
@Service
public class ConfigService {

    private final AppConfigRepository repository;

    public ConfigService(final AppConfigRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Labels labels() {
        return new Labels(repository.findAll().stream()
                .collect(Collectors.toMap(AppConfigEntry::getKey, AppConfigEntry::getValue)));
    }

    /**
     * Enregistre les valeurs saisies, comme l'original : création des clés absentes
     * (si une valeur est saisie), mise à jour des clés existantes modifiées.
     *
     * @return nombre de changements effectués
     */
    @Transactional
    public int save(final CurrentUser actor, final Map<String, String> submitted) {
        requireOwner(actor);
        final Map<String, AppConfigEntry> existing = repository.findAll().stream()
                .collect(Collectors.toMap(AppConfigEntry::getKey, entry -> entry));
        final Set<String> editable = Set.copyOf(SettingsCatalog.allKeys());
        int changes = 0;
        for (final Map.Entry<String, String> edit : submitted.entrySet()) {
            if (editable.contains(edit.getKey())) {
                changes += apply(existing.get(edit.getKey()), edit.getKey(), edit.getValue());
            }
        }
        return changes;
    }

    /** Vide toutes les valeurs : les libellés par défaut reprennent la main. */
    @Transactional
    public void reset(final CurrentUser actor) {
        requireOwner(actor);
        final List<AppConfigEntry> entries = repository.findAll();
        entries.forEach(entry -> entry.changeValue(""));
    }

    private int apply(final AppConfigEntry current, final String key, final String value) {
        if (current == null) {
            if (value.isBlank()) {
                return 0;
            }
            repository.save(new AppConfigEntry(key, value));
            return 1;
        }
        if (current.getValue().equals(value)) {
            return 0;
        }
        current.changeValue(value);
        return 1;
    }

    private static void requireOwner(final CurrentUser actor) {
        if (!actor.isRealOwner()) {
            throw new AccessDeniedException("Réservé à l'owner");
        }
    }
}
