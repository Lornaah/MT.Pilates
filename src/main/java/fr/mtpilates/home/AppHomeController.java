package fr.mtpilates.home;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Accueil de l'espace connecté. Les tableaux de bord (membre, staff) arrivent au module M9. */
@Controller
public class AppHomeController {

    @GetMapping("/app")
    public String home() {
        return "pages/app-home";
    }
}
