package fr.mtpilates.nutrition;

import fr.mtpilates.auth.CurrentUser;
import fr.mtpilates.auth.CurrentUserService;
import fr.mtpilates.shared.ui.JsonWriter;
import fr.mtpilates.shared.web.Flash;
import fr.mtpilates.shared.web.Toast;
import fr.mtpilates.shared.web.ToastResponses;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Générateur de plan repas (/app/coach/nutrition/plans/generate). L'état de l'assistant vit dans Alpine ;
 * la génération, la liste de courses et l'envoi passent par le serveur en JSON.
 */
@Controller
public class MealPlanGeneratorController {

    private static final String PAGE = "/app/coach/nutrition/plans/generate";

    private final MealPlanGeneratorService generator;
    private final RecipeService recipes;
    private final CurrentUserService currentUsers;
    private final ToastResponses toasts;
    private final JsonWriter json;

    public MealPlanGeneratorController(final MealPlanGeneratorService generator, final RecipeService recipes,
                                       final CurrentUserService currentUsers, final ToastResponses toasts,
                                       final JsonWriter json) {
        this.generator = generator;
        this.recipes = recipes;
        this.currentUsers = currentUsers;
        this.toasts = toasts;
        this.json = json;
    }

    @GetMapping(PAGE)
    public String page(@RequestParam(required = false) final UUID member, final Model model) {
        final CurrentUser user = currentUsers.require();
        model.addAttribute("membersJson", json.write(generator.members(user)));
        model.addAttribute("recipesJson", json.write(recipes.options()));
        model.addAttribute("presetMember", member == null ? "" : member.toString());
        model.addAttribute("dietaryTags", NutritionCatalog.DIETARY_TAGS);
        model.addAttribute("allergens", NutritionCatalog.ALLERGENS);
        model.addAttribute("equipment", NutritionCatalog.EQUIPMENT);
        model.addAttribute("mealTypes", NutritionCatalog.MEAL_TYPES);
        model.addAttribute("days", NutritionCatalog.DAYS);
        return "pages/meal-plan-generator";
    }

    @GetMapping("/app/coach/meal-plans/generate")
    public String legacyRedirect() {
        return "redirect:" + PAGE;
    }

    /** Toute la semaine ; « Régénérer » sur un jour n'en garde que ce jour, côté assistant, comme l'original. */
    @PostMapping(PAGE + "/run")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> run(@RequestBody final GeneratorRequest request,
                                                   final HttpServletResponse response) {
        final CurrentUser user = currentUsers.require();
        if (!generator.hasRecipes()) {
            toasts.trigger(response, Toast.error("Aucune recette en base — créez-en avant de générer"));
            return ResponseEntity.noContent().build();
        }
        final Map<String, Object> body = new LinkedHashMap<>();
        body.put("meals", generator.generate(user, request));
        return ResponseEntity.ok(body);
    }

    @PostMapping(PAGE + "/shopping")
    @ResponseBody
    public List<Map<String, Object>> shopping(@RequestBody final GeneratorRequest request) {
        return generator.shoppingList(currentUsers.require(), request.meals() == null ? Map.of() : request.meals());
    }

    @PostMapping("/app/coach/nutrition/plans")
    public ResponseEntity<Void> send(@RequestBody final GeneratedPlan plan, final HttpServletRequest request,
                                     final HttpServletResponse response) {
        try {
            generator.send(currentUsers.require(), plan);
        } catch (final IllegalArgumentException e) {
            toasts.trigger(response, Toast.error(e.getMessage()));
            return ResponseEntity.noContent().build();
        }
        final String target = "/app/coach/members/" + plan.memberId();
        Flash.put(request, response, target, Map.of(ToastResponses.FLASH_LIST_ATTRIBUTE,
                List.of(Toast.success("Plan repas créé"), Toast.success("Plan envoyé à l'élève"))));
        return ResponseEntity.noContent().header("HX-Redirect", target).build();
    }
}
