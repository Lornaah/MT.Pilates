package fr.mtpilates.shared.ui;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Expose les helpers de rendu à tous les templates. Thymeleaf 3.1 interdit les
 * références directes aux beans ({@code @icons}) dans plusieurs contextes,
 * d'où le passage par le modèle.
 */
@ControllerAdvice
public class UiModelAdvice {

    private final IconRegistry icons;
    private final JsonWriter json;

    public UiModelAdvice(final IconRegistry icons, final JsonWriter json) {
        this.icons = icons;
        this.json = json;
    }

    @ModelAttribute("icons")
    public IconRegistry icons() {
        return icons;
    }

    @ModelAttribute("json")
    public JsonWriter json() {
        return json;
    }
}
