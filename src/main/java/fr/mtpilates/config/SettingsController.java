package fr.mtpilates.config;

import fr.mtpilates.auth.CurrentUser;
import fr.mtpilates.auth.CurrentUserService;
import fr.mtpilates.layout.SidebarFactory;
import fr.mtpilates.shared.web.Toast;
import fr.mtpilates.shared.web.ToastResponses;
import jakarta.servlet.http.HttpServletResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Écran Configuration (/app/owner/settings) : libellés personnalisables par l'owner. */
@Controller
@RequestMapping("/app/owner/settings")
public class SettingsController {

    static final String FIELD_PREFIX = "cfg.";
    private static final String PATH = "/app/owner/settings";

    private final ConfigService config;
    private final CurrentUserService currentUsers;
    private final SidebarFactory sidebars;
    private final ToastResponses toasts;

    public SettingsController(final ConfigService config, final CurrentUserService currentUsers,
                              final SidebarFactory sidebars, final ToastResponses toasts) {
        this.config = config;
        this.currentUsers = currentUsers;
        this.sidebars = sidebars;
        this.toasts = toasts;
    }

    @GetMapping
    public String page(final Model model) {
        model.addAttribute("sections", SettingsCatalog.SECTIONS);
        return "pages/settings";
    }

    @PostMapping
    public String save(@RequestParam final Map<String, String> params, final HttpServletResponse response,
                       final Model model) {
        final int changes = config.save(currentUsers.require(), submittedValues(params));
        toasts.trigger(response, Toast.success("Configuration enregistrée (" + changes + " changement(s))"));
        refreshSidebar(model);
        return "pages/settings :: saved";
    }

    @PostMapping("/reset")
    public String reset(final HttpServletResponse response, final Model model) {
        config.reset(currentUsers.require());
        toasts.trigger(response, Toast.success("Valeurs remises aux défauts"));
        model.addAttribute("sections", SettingsCatalog.SECTIONS);
        refreshSidebar(model);
        return "pages/settings :: resetResult";
    }

    /** Les libellés viennent de changer : la barre latérale est renvoyée hors bande (HTMX) avec les nouveaux. */
    private void refreshSidebar(final Model model) {
        final Labels labels = config.labels();
        final CurrentUser user = currentUsers.require();
        model.addAttribute("labels", labels);
        model.addAttribute("sidebar", Optional.of(sidebars.build(user, labels, PATH)));
        model.addAttribute("sidebarOob", "true");
    }

    private static Map<String, String> submittedValues(final Map<String, String> params) {
        final Map<String, String> values = new LinkedHashMap<>();
        params.forEach((name, value) -> {
            if (name.startsWith(FIELD_PREFIX)) {
                values.put(name.substring(FIELD_PREFIX.length()), value);
            }
        });
        return values;
    }
}
