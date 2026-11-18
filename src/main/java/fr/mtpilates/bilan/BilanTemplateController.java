package fr.mtpilates.bilan;

import fr.mtpilates.auth.CurrentUserService;
import fr.mtpilates.shared.web.Toast;
import fr.mtpilates.shared.web.ToastResponses;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Modèle de textes du bilan, modifiable par l'owner (/app/owner/bilan-template). */
@Controller
public class BilanTemplateController {

    private static final String PATH = "/app/owner/bilan-template";

    private final BilanTemplateService templates;
    private final CurrentUserService currentUsers;
    private final ToastResponses toasts;

    public BilanTemplateController(final BilanTemplateService templates, final CurrentUserService currentUsers,
                                   final ToastResponses toasts) {
        this.templates = templates;
        this.currentUsers = currentUsers;
        this.toasts = toasts;
    }

    @GetMapping(PATH)
    public String page(final Model model) {
        model.addAttribute("sections", BilanTemplateCatalog.SECTIONS);
        model.addAttribute("texts", templates.current());
        model.addAttribute("variables", BilanTemplateCatalog.VARIABLES);
        return "pages/bilan-template";
    }

    @PostMapping(PATH)
    public ResponseEntity<Void> save(@RequestParam final Map<String, String> params, final HttpServletResponse response) {
        templates.save(currentUsers.require(), params);
        toasts.trigger(response, Toast.success("Modèle de bilan enregistré"));
        return ResponseEntity.noContent().build();
    }

    @PostMapping(PATH + "/reset")
    public ResponseEntity<Void> reset(final HttpServletResponse response) {
        templates.reset(currentUsers.require());
        response.setHeader("HX-Refresh", "true");
        return ResponseEntity.noContent().build();
    }
}
