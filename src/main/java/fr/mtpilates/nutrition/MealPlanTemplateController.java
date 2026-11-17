package fr.mtpilates.nutrition;

import fr.mtpilates.auth.CurrentUser;
import fr.mtpilates.auth.CurrentUserService;
import fr.mtpilates.shared.ui.JsonWriter;
import fr.mtpilates.shared.web.Flash;
import fr.mtpilates.shared.web.Toast;
import fr.mtpilates.shared.web.ToastResponses;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

/** Menus types : éditeur (création, modification, suppression) et assignation à un élève. */
@Controller
@RequestMapping("/app/coach/nutrition/templates")
public class MealPlanTemplateController {

    private static final String VIEW = "fragments/nutrition/templates";
    private static final String EDITOR = "/app/coach/nutrition/templates/";
    private static final String LIBRARY = "/app/coach/nutrition?tab=templates";

    private final MealPlanTemplateService templates;
    private final RecipeService recipes;
    private final CurrentUserService currentUsers;
    private final ToastResponses toasts;
    private final JsonWriter json;

    public MealPlanTemplateController(final MealPlanTemplateService templates, final RecipeService recipes,
                                      final CurrentUserService currentUsers, final ToastResponses toasts,
                                      final JsonWriter json) {
        this.templates = templates;
        this.recipes = recipes;
        this.json = json;
        this.currentUsers = currentUsers;
        this.toasts = toasts;
    }

    @GetMapping({"/new", "/{id}"})
    public String editor(@PathVariable(required = false) final UUID id, final Model model) {
        final Map<String, Object> state = templates.editorState(currentUsers.require(), Optional.ofNullable(id))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("templateId", id == null ? "" : id.toString());
        model.addAttribute("isNew", id == null);
        model.addAttribute("stateJson", json.write(state));
        model.addAttribute("recipesJson", json.write(recipes.options()));
        model.addAttribute("covers", TemplateCovers.ALL);
        model.addAttribute("regimes", NutritionCatalog.REGIMES);
        model.addAttribute("mealTypes", NutritionCatalog.MEAL_TYPES);
        model.addAttribute("mealGridDays", NutritionCatalog.DAYS);
        return "pages/meal-plan-template-editor";
    }

    /** Création : on rejoint l'URL du menu ; mise à jour : on reste sur l'éditeur (état Alpine conservé). */
    @PostMapping("/save")
    public ResponseEntity<Void> save(@RequestParam(defaultValue = "") final String id, @RequestParam final String payload,
                                     @RequestParam(defaultValue = "draft") final String status,
                                     final HttpServletRequest request, final HttpServletResponse response) {
        final Optional<UUID> existing = id.isBlank() ? Optional.empty() : Optional.of(UUID.fromString(id));
        try {
            final UUID saved = templates.save(currentUsers.require(), existing, payload, status);
            if (existing.isPresent()) {
                toasts.trigger(response, Toast.success("Menu type mis à jour"));
                return ResponseEntity.noContent().build();
            }
            Flash.put(request, response, EDITOR + saved,
                    Map.of(ToastResponses.FLASH_ATTRIBUTE, Toast.success("Menu type créé")));
            return ResponseEntity.noContent().header("HX-Redirect", EDITOR + saved).build();
        } catch (final IllegalArgumentException e) {
            toasts.trigger(response, Toast.error(e.getMessage()));
            return ResponseEntity.noContent().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable final UUID id, final HttpServletRequest request,
                                       final HttpServletResponse response) {
        templates.delete(currentUsers.require(), id);
        Flash.put(request, response, LIBRARY, Map.of(ToastResponses.FLASH_ATTRIBUTE, Toast.success("Menu type supprimé")));
        return ResponseEntity.noContent().header("HX-Redirect", LIBRARY).build();
    }

    @GetMapping("/{id}/assign")
    public String assignDialog(@PathVariable final UUID id, final Model model) {
        final CurrentUser user = currentUsers.require();
        model.addAttribute("template", templates.card(user, id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)));
        model.addAttribute("members", templates.assignableMembers(user));
        return VIEW + " :: assignDialog";
    }

    @PostMapping("/{id}/assign")
    public String assign(@PathVariable final UUID id, @RequestParam final UUID memberId,
                         @RequestParam(defaultValue = "0") final int weekIndex, final HttpServletResponse response,
                         final Model model) {
        final CurrentUser user = currentUsers.require();
        try {
            templates.assign(user, id, memberId, weekIndex);
            toasts.trigger(response, Toast.success("Plan assigné à l'élève"), Map.of("dialog-close", "template-assign"));
        } catch (final IllegalArgumentException e) {
            toasts.trigger(response, Toast.error(e.getMessage()));
            response.setHeader("HX-Reswap", "none");
        }
        NutritionHubController.fillTemplates(model, templates.library(user));
        return VIEW + " :: library";
    }
}
