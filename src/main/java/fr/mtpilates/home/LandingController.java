package fr.mtpilates.home;

import java.time.Clock;
import java.time.Year;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** Page d'accueil publique (contenu marketing statique). */
@Controller
public class LandingController {

    private static final List<Pillar> PILLARS = List.of(
            new Pillar("circle-play", "Bibliothèque vidéo",
                    "Des séances guidées par Marion, classées par niveau et par focus (dos, posture, prénatal, énergie…)."),
            new Pillar("calendar", "Planning sur-mesure",
                    "Réservez vos cours collectifs, vos sessions individuelles, et gardez un œil sur votre rythme hebdomadaire."),
            new Pillar("heart", "Suivi attentif",
                    "Notes de coach, objectifs personnels, mensurations, journal de séance : tout est centralisé."));

    private final Clock clock;

    public LandingController(final Clock clock) {
        this.clock = clock;
    }

    @GetMapping("/")
    public String landing(final Model model) {
        model.addAttribute("pillars", PILLARS);
        model.addAttribute("year", Year.now(clock).getValue());
        return "pages/index";
    }

    public record Pillar(String icon, String title, String text) {
    }
}
