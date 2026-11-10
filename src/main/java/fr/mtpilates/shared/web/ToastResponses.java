package fr.mtpilates.shared.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.json.JsonWriteFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import jakarta.servlet.http.HttpServletResponse;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Transmet un toast au navigateur : par l'en-tête {@code HX-Trigger} pour une
 * réponse HTMX, par un attribut flash pour une redirection classique.
 */
@Component
public class ToastResponses {

    public static final String FLASH_ATTRIBUTE = "toast";
    private static final String HX_TRIGGER = "HX-Trigger";

    private final ObjectWriter headerWriter;

    public ToastResponses(final ObjectMapper objectMapper) {
        // Un en-tête HTTP n'accepte que l'ASCII (Tomcat le supprime sinon) : accents et « … », « • » sont échappés en JSON.
        this.headerWriter = objectMapper.writer().with(JsonWriteFeature.ESCAPE_NON_ASCII);
    }

    public void trigger(final HttpServletResponse response, final Toast toast) {
        response.setHeader(HX_TRIGGER, toJson(toast, Map.of()));
    }

    /** Toast accompagné d'autres événements HTMX (écoutés côté Alpine, ex. changer d'onglet). */
    public void trigger(final HttpServletResponse response, final Toast toast, final Map<String, Object> otherEvents) {
        response.setHeader(HX_TRIGGER, toJson(toast, otherEvents));
    }

    public void flash(final RedirectAttributes attributes, final Toast toast) {
        attributes.addFlashAttribute(FLASH_ATTRIBUTE, toast);
    }

    private String toJson(final Toast toast, final Map<String, Object> otherEvents) {
        final Map<String, Object> events = new LinkedHashMap<>(otherEvents);
        events.put("toast", Map.of(
                "message", toast.message(),
                "category", toast.category().name().toLowerCase(Locale.ROOT)));
        try {
            return headerWriter.writeValueAsString(events);
        } catch (final JsonProcessingException e) {
            throw new IllegalStateException("Sérialisation du toast impossible", e);
        }
    }
}
