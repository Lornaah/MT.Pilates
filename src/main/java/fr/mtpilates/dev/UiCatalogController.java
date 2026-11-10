package fr.mtpilates.dev;

import fr.mtpilates.shared.web.Toast;
import fr.mtpilates.shared.web.ToastResponses;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Set;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Catalogue des composants (profil dev uniquement) : sert à la recette visuelle
 * et prouve qu'un thème s'applique sans toucher aux templates (?theme=dark).
 */
@Controller
@Profile("dev")
@RequestMapping("/dev/ui")
public class UiCatalogController {

    private static final Set<String> THEMES = Set.of("default", "dark");

    private final ToastResponses toasts;

    public UiCatalogController(final ToastResponses toasts) {
        this.toasts = toasts;
    }

    @GetMapping
    public String catalog(@RequestParam(defaultValue = "default") final String theme, final Model model) {
        model.addAttribute("theme", THEMES.contains(theme) ? theme : "default");
        return "dev/ui";
    }

    @PostMapping("/toast")
    public ResponseEntity<Void> toast(@RequestParam final String category, final HttpServletResponse response) {
        final Toast toast = switch (category) {
            case "success" -> Toast.success("Fiche mise à jour");
            case "error" -> Toast.error("Erreur inattendue");
            default -> Toast.info("Aucun résultat");
        };
        toasts.trigger(response, toast);
        return ResponseEntity.noContent().build();
    }
}
