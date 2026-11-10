package fr.mtpilates.shared.web;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Thème actif, posé en {@code data-theme} sur la balise html. Seul le thème par
 * défaut existe pour l'instant : le point d'extension est ici.
 */
@ControllerAdvice
public class ThemeModelAdvice {

    public static final String DEFAULT_THEME = "default";

    @ModelAttribute("theme")
    public String theme() {
        return DEFAULT_THEME;
    }
}
