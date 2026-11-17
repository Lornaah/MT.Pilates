package fr.mtpilates.nutrition;

import fr.mtpilates.auth.CurrentUserService;
import fr.mtpilates.shared.web.Toast;
import fr.mtpilates.shared.web.ToastResponses;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Recettes : dialog de création / modification et suppression. */
@Controller
@RequestMapping("/app/coach/nutrition/recipes")
public class RecipeController {

    private static final String VIEW = "fragments/nutrition/recipes";

    private final RecipeService recipes;
    private final CurrentUserService currentUsers;
    private final ToastResponses toasts;

    public RecipeController(final RecipeService recipes, final CurrentUserService currentUsers, final ToastResponses toasts) {
        this.recipes = recipes;
        this.currentUsers = currentUsers;
        this.toasts = toasts;
    }

    @GetMapping({"/new", "/{id}/edit"})
    public String form(@PathVariable(required = false) final UUID id, final Model model) {
        model.addAttribute("recipeId", id);
        model.addAttribute("recipeState", recipes.formState(currentUsers.require(), Optional.ofNullable(id)));
        NutritionHubController.fillRecipes(model, new RecipeLibrary(java.util.List.of(), 0, 0));
        return VIEW + " :: recipeDialog";
    }

    @PostMapping({"", "/{id}"})
    public String save(@PathVariable(required = false) final UUID id, @RequestParam(defaultValue = "{}") final String payload,
                       final HttpServletResponse response, final Model model) {
        try {
            recipes.save(currentUsers.require(), Optional.ofNullable(id), payload);
            toasts.trigger(response, Toast.success(id == null ? "Recette créée" : "Recette mise à jour"),
                    Map.of("dialog-close", "recipe-form"));
        } catch (final IllegalArgumentException e) {
            toasts.trigger(response, Toast.error(e.getMessage()));
            response.setHeader("HX-Reswap", "none");
        }
        return refresh(model);
    }

    @DeleteMapping("/{id}")
    public String delete(@PathVariable final UUID id, final HttpServletResponse response, final Model model) {
        recipes.delete(currentUsers.require(), id);
        toasts.trigger(response, Toast.success("Recette supprimée"));
        return refresh(model);
    }

    private String refresh(final Model model) {
        NutritionHubController.fillRecipes(model, recipes.library(currentUsers.require()));
        return VIEW + " :: refresh";
    }
}
