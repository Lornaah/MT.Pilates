package fr.mtpilates.training;

import fr.mtpilates.auth.CurrentUser;
import fr.mtpilates.auth.CurrentUserService;
import fr.mtpilates.shared.ui.JsonWriter;
import fr.mtpilates.shared.web.Toast;
import fr.mtpilates.shared.web.ToastResponses;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/** Bibliothèque d'exercices (/app/coach/exercises, aussi onglet « Exercices » du hub entraînement). */
@Controller
@RequestMapping("/app/coach/exercises")
public class ExerciseController {

    private static final String RESULTS = "fragments/training/exercises :: results";
    private static final String DIALOG = "fragments/training/exercises :: exerciseDialog";
    private static final ExerciseContent EMPTY = new ExerciseContent("", "", "", "", "", List.of(), List.of(), "",
            List.of(), List.of(), true);

    private final ExerciseService exercises;
    private final CurrentUserService currentUsers;
    private final ToastResponses toasts;
    private final JsonWriter json;

    public ExerciseController(final ExerciseService exercises, final CurrentUserService currentUsers,
                              final ToastResponses toasts, final JsonWriter json) {
        this.exercises = exercises;
        this.currentUsers = currentUsers;
        this.toasts = toasts;
        this.json = json;
    }

    @GetMapping
    public String page(final Model model) {
        model.addAttribute("library", exercises.library(currentUsers.require()));
        return "pages/exercises";
    }

    @GetMapping("/new")
    public String newDialog(final Model model) {
        return dialog(model, Optional.empty(), EMPTY);
    }

    @GetMapping("/{id}/edit")
    public String editDialog(@PathVariable final UUID id, final Model model) {
        return dialog(model, Optional.of(id), exercises.content(currentUsers.require(), id).orElse(EMPTY));
    }

    @PostMapping
    public String create(@ModelAttribute final ExerciseForm form, final HttpServletResponse response, final Model model) {
        return save(Optional.empty(), form, "Exercice créé", response, model);
    }

    @PostMapping("/{id}")
    public String update(@PathVariable final UUID id, @ModelAttribute final ExerciseForm form,
                         final HttpServletResponse response, final Model model) {
        return save(Optional.of(id), form, "Exercice mis à jour", response, model);
    }

    @PostMapping("/{id}/favorite")
    public String toggleFavorite(@PathVariable final UUID id, final Model model) {
        exercises.toggleFavorite(currentUsers.require(), id);
        return results(model);
    }

    @PostMapping("/{id}/duplicate")
    public String duplicate(@PathVariable final UUID id, final HttpServletResponse response, final Model model) {
        exercises.duplicate(currentUsers.require(), id);
        toasts.trigger(response, Toast.success("Exercice dupliqué"));
        return results(model);
    }

    @DeleteMapping("/{id}")
    public String delete(@PathVariable final UUID id, final HttpServletResponse response, final Model model) {
        exercises.delete(currentUsers.require(), id);
        toasts.trigger(response, Toast.success("Exercice supprimé"));
        return results(model);
    }

    private String save(final Optional<UUID> id, final ExerciseForm form, final String message,
                        final HttpServletResponse response, final Model model) {
        if (exercises.save(currentUsers.require(), id, form)) {
            toasts.trigger(response, Toast.success(message), Map.of("dialog-close", "exercise-form"));
        } else {
            response.setHeader("HX-Reswap", "none");
        }
        return results(model);
    }

    private String results(final Model model) {
        model.addAttribute("library", exercises.library(currentUsers.require()));
        return RESULTS;
    }

    private String dialog(final Model model, final Optional<UUID> id, final ExerciseContent content) {
        final CurrentUser user = currentUsers.require();
        final ExerciseLibraryView library = exercises.library(user);
        model.addAttribute("exerciseId", id.map(UUID::toString).orElse(null));
        model.addAttribute("form", content);
        model.addAttribute("attributesJson", ExerciseAttributes.toJson(content.defaultAttributes()));
        model.addAttribute("attributeTypesJson", json.write(attributeTypes()));
        model.addAttribute("muscleSuggestions", ExerciseCatalog.MUSCLE_GROUPS);
        model.addAttribute("equipmentSuggestions", ExerciseCatalog.EQUIPMENT);
        model.addAttribute("typeOptions", library.typeOptions());
        return DIALOG;
    }

    static List<Map<String, Object>> attributeTypes() {
        return Arrays.stream(ExerciseAttributeType.values()).map(t -> {
            final Map<String, Object> m = new LinkedHashMap<>();
            m.put("code", t.code());
            m.put("label", t.label());
            m.put("unit", t.unit().orElse(null));
            m.put("kind", t.kind().name());
            m.put("defaultValue", t.defaultValue().orElse(null));
            return m;
        }).toList();
    }
}
