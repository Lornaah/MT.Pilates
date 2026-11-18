package fr.mtpilates.bilan;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.mtpilates.auth.CurrentUser;
import fr.mtpilates.config.AppConfigEntry;
import fr.mtpilates.config.AppConfigRepository;
import fr.mtpilates.user.AppRole;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Modèle de textes du bilan : textes par défaut du catalogue, remplacés par ceux enregistrés par l'owner
 * (clé `bilan_template` d'app_config, en JSON). Les nouveaux bilans recopient le modèle courant.
 */
@Service
public class BilanTemplateService {

    static final String CONFIG_KEY = "bilan_template";
    private static final TypeReference<Map<String, String>> MAP = new TypeReference<>() { };

    private final AppConfigRepository config;
    private final ObjectMapper json;

    public BilanTemplateService(final AppConfigRepository config, final ObjectMapper json) {
        this.config = config;
        this.json = json;
    }

    @Transactional(readOnly = true)
    public Map<String, String> current() {
        final Map<String, String> texts = BilanTemplateCatalog.defaults();
        config.findByKey(CONFIG_KEY).map(AppConfigEntry::getValue).map(this::parse).ifPresent(saved ->
                saved.forEach((key, value) -> {
                    if (texts.containsKey(key) && value != null) {
                        texts.put(key, value);
                    }
                }));
        return texts;
    }

    /** Enregistre le modèle : seuls les textes différents du défaut sont conservés. */
    @Transactional
    public void save(final CurrentUser owner, final Map<String, String> submitted) {
        requireOwner(owner);
        final Map<String, String> defaults = BilanTemplateCatalog.defaults();
        final Map<String, String> changed = new LinkedHashMap<>();
        defaults.forEach((key, value) -> {
            final String text = submitted.get(key);
            if (text != null && !normalize(text).equals(value)) {
                changed.put(key, normalize(text));
            }
        });
        final String value = write(changed);
        config.findByKey(CONFIG_KEY).ifPresentOrElse(e -> e.changeValue(value),
                () -> config.save(new AppConfigEntry(CONFIG_KEY, value)));
    }

    @Transactional
    public void reset(final CurrentUser owner) {
        requireOwner(owner);
        config.findByKey(CONFIG_KEY).ifPresent(config::delete);
    }

    /** Les zones de texte envoient des fins de ligne CRLF : on garde le format du catalogue. */
    private static String normalize(final String text) {
        return text.replace("\r\n", "\n");
    }

    private static void requireOwner(final CurrentUser user) {
        if (!user.realRoles().contains(AppRole.OWNER)) {
            throw new AccessDeniedException("Réservé à l'owner");
        }
    }

    private Map<String, String> parse(final String value) {
        try {
            return json.readValue(value, MAP);
        } catch (final JsonProcessingException e) {
            return Map.of();
        }
    }

    private String write(final Map<String, String> texts) {
        try {
            return json.writeValueAsString(texts);
        } catch (final JsonProcessingException e) {
            throw new IllegalStateException("Modèle de bilan non sérialisable", e);
        }
    }
}
