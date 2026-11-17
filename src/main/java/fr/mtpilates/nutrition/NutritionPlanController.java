package fr.mtpilates.nutrition;

import fr.mtpilates.auth.CurrentUser;
import fr.mtpilates.auth.CurrentUserService;
import fr.mtpilates.shared.ui.JsonWriter;
import fr.mtpilates.shared.web.Toast;
import fr.mtpilates.shared.web.ToastResponses;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

/** Plan nutritionnel : éditeur coach par membre et page « Ton plan nutritionnel » du membre. */
@Controller
public class NutritionPlanController {

    private final NutritionEditorService editor;
    private final MyNutritionService myNutrition;
    private final CurrentUserService currentUsers;
    private final ToastResponses toasts;
    private final JsonWriter json;

    public NutritionPlanController(final NutritionEditorService editor, final MyNutritionService myNutrition,
                                   final CurrentUserService currentUsers, final ToastResponses toasts,
                                   final JsonWriter json) {
        this.editor = editor;
        this.myNutrition = myNutrition;
        this.currentUsers = currentUsers;
        this.toasts = toasts;
        this.json = json;
    }

    @GetMapping("/app/nutrition")
    public String myPlan(final Model model) {
        model.addAttribute("plan", myNutrition.activePlan(currentUsers.require()));
        return "pages/my-nutrition";
    }

    @GetMapping("/app/coach/members/{memberId}/nutrition")
    public String editor(@PathVariable final UUID memberId, final Model model) {
        final CurrentUser user = currentUsers.require();
        model.addAttribute("memberId", memberId);
        model.addAttribute("memberName", editor.memberName(user, memberId));
        model.addAttribute("stateJson", json.write(editor.state(user, memberId)));
        model.addAttribute("recipesJson", json.write(editor.recipeChoices()));
        model.addAttribute("days", NutritionCatalog.DAYS);
        model.addAttribute("units", NutritionCatalog.UNITS);
        return "pages/nutrition-editor";
    }

    @PostMapping("/app/coach/members/{memberId}/nutrition")
    @ResponseBody
    public Map<String, String> save(@PathVariable final UUID memberId, @RequestBody final NutritionEditorService.EditedPlan plan,
                                    final HttpServletResponse response) {
        final UUID id = editor.save(currentUsers.require(), memberId, plan);
        toasts.trigger(response, Toast.success("Plan enregistré"));
        return Map.of("id", id.toString());
    }
}
