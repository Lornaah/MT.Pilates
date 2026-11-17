package fr.mtpilates.profile;

import fr.mtpilates.auth.CurrentUser;
import fr.mtpilates.auth.CurrentUserService;
import fr.mtpilates.config.ConfigService;
import fr.mtpilates.shared.web.Toast;
import fr.mtpilates.shared.web.ToastResponses;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Parcours de première connexion d'un membre (/onboarding). */
@Controller
@RequestMapping("/onboarding")
public class OnboardingController {

    private final ProfileService profiles;
    private final CurrentUserService currentUsers;
    private final ConfigService config;

    public OnboardingController(final ProfileService profiles, final CurrentUserService currentUsers,
                                final ConfigService config) {
        this.profiles = profiles;
        this.currentUsers = currentUsers;
        this.config = config;
    }

    @GetMapping
    public String page(final Model model) {
        final CurrentUser user = currentUsers.require();
        if (!profiles.needsOnboarding(user)) {
            return "redirect:/app";
        }
        model.addAttribute("profile", profiles.own(user));
        model.addAttribute("videoEmbedUrl", VideoEmbed.of(config.labels().raw("onboarding_video_url")));
        return "pages/onboarding";
    }

    @PostMapping
    public String complete(@ModelAttribute final ProfileForm form, final RedirectAttributes redirect) {
        final CurrentUser user = currentUsers.require();
        if (profiles.needsOnboarding(user)) {
            profiles.completeOnboarding(user, form);
            // L'original affichait deux toasts : celui de la mise à jour de fiche puis celui de bienvenue.
            redirect.addFlashAttribute(ToastResponses.FLASH_LIST_ATTRIBUTE, List.of(
                    Toast.success("Fiche mise à jour"), Toast.success("Bienvenue ! Votre espace est prêt.")));
        }
        return "redirect:/app";
    }
}
