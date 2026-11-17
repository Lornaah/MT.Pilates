package fr.mtpilates.user;

import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Expose les listes de choix du profil aux vues : Thymeleaf interdit l'accès aux classes statiques dans les fragments. */
@ControllerAdvice
public class ProfileOptionsAdvice {

    /** Listes de choix telles que les formulaires de profil les affichent. */
    public record Options(Map<String, String> sedentarity, Map<String, String> bodyTypes, List<String> equipment,
                          Map<String, String> days, Map<String, String> slots, List<String> dietTypes,
                          Map<String, String> cookingTimes) {
    }

    private static final Options OPTIONS = new Options(ProfileOptions.SEDENTARITY, ProfileOptions.BODY_TYPES,
            ProfileOptions.EQUIPMENT, ProfileOptions.DAYS, ProfileOptions.SLOTS, ProfileOptions.DIET_TYPES,
            ProfileOptions.COOKING_TIMES);

    @ModelAttribute("profileOptions")
    public Options profileOptions() {
        return OPTIONS;
    }
}
