package fr.mtpilates.shared.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
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

    private final ObjectMapper objectMapper;

    public ToastResponses(final ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void trigger(final HttpServletResponse response, final Toast toast) {
        response.setHeader(HX_TRIGGER, toJson(toast));
    }

    public void flash(final RedirectAttributes attributes, final Toast toast) {
        attributes.addFlashAttribute(FLASH_ATTRIBUTE, toast);
    }

    private String toJson(final Toast toast) {
        final Map<String, Object> event = Map.of("toast", Map.of(
                "message", toast.message(),
                "category", toast.category().name().toLowerCase(Locale.ROOT)));
        try {
            return objectMapper.writeValueAsString(event);
        } catch (final JsonProcessingException e) {
            throw new IllegalStateException("Sérialisation du toast impossible", e);
        }
    }
}
