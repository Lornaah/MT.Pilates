package fr.mtpilates.training;

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
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Bibliothèque de séances (/app/coach/sessions) et éditeur par blocs (/app/coach/sessions/new, /{id}). */
@Controller
@RequestMapping("/app/coach/sessions")
public class SessionController {

    private static final String EDITOR_PATH = "/app/coach/sessions/";

    private final SessionService sessions;
    private final CurrentUserService currentUsers;
    private final ToastResponses toasts;
    private final JsonWriter json;

    public SessionController(final SessionService sessions, final CurrentUserService currentUsers,
                             final ToastResponses toasts, final JsonWriter json) {
        this.sessions = sessions;
        this.currentUsers = currentUsers;
        this.toasts = toasts;
        this.json = json;
    }

    @GetMapping
    public String library(final Model model) {
        fillLibrary(model, sessions, currentUsers.require());
        return "pages/sessions";
    }

    @GetMapping("/new")
    public String create(final Model model) {
        return editor(model, Optional.empty());
    }

    @GetMapping("/{id}")
    public String edit(@PathVariable final UUID id, final Model model) {
        return editor(model, Optional.of(id));
    }

    /** Après l'enregistrement, on recharge l'éditeur de la séance (navigate replace de l'original). */
    @PostMapping("/save")
    public ResponseEntity<Void> save(@RequestParam(defaultValue = "") final String id,
                                     @RequestParam final String payload, final HttpServletRequest request,
                                     final HttpServletResponse response) {
        try {
            final UUID saved = sessions.save(currentUsers.require(), parse(id), payload);
            flash(request, response, saved, "Séance enregistrée");
            return ResponseEntity.noContent().header("HX-Redirect", EDITOR_PATH + saved).build();
        } catch (final SessionValidationException e) {
            toasts.trigger(response, Toast.error(e.getMessage()));
            return ResponseEntity.noContent().build();
        }
    }

    @PostMapping("/{id}/duplicate")
    public ResponseEntity<Void> duplicate(@PathVariable final UUID id, final HttpServletRequest request,
                                          final HttpServletResponse response) {
        final UUID copy = sessions.duplicate(currentUsers.require(), id);
        flash(request, response, copy, "Séance dupliquée");
        return ResponseEntity.noContent().header("HX-Redirect", EDITOR_PATH + copy).build();
    }

    @DeleteMapping("/{id}")
    public String delete(@PathVariable final UUID id, final HttpServletResponse response, final Model model) {
        final CurrentUser user = currentUsers.require();
        sessions.delete(user, id);
        toasts.trigger(response, Toast.success("Séance supprimée"));
        fillLibrary(model, sessions, user);
        return "fragments/training/sessions :: grid";
    }

    static void fillLibrary(final Model model, final SessionService sessions, final CurrentUser user) {
        model.addAttribute("sessions", sessions.library(user));
        model.addAttribute("categories", sessions.categories());
        model.addAttribute("hasExercises", sessions.hasExercises());
        model.addAttribute("levels", SessionLevel.values());
    }

    private String editor(final Model model, final Optional<UUID> id) {
        final CurrentUser user = currentUsers.require();
        model.addAttribute("sessionId", id.map(UUID::toString).orElse(""));
        model.addAttribute("isNew", id.isEmpty());
        model.addAttribute("stateJson", json.write(sessions.editorState(user, id)));
        model.addAttribute("exercisesJson", json.write(sessions.exerciseOptions()));
        model.addAttribute("attributeTypesJson", json.write(ExerciseController.attributeTypes()));
        model.addAttribute("categories", sessions.categories());
        model.addAttribute("levels", SessionLevel.values());
        return "pages/session-editor";
    }

    private static void flash(final HttpServletRequest request, final HttpServletResponse response, final UUID id,
                              final String message) {
        Flash.put(request, response, EDITOR_PATH + id, Map.of(ToastResponses.FLASH_ATTRIBUTE, Toast.success(message)));
    }

    private static Optional<UUID> parse(final String id) {
        try {
            return id.isBlank() ? Optional.empty() : Optional.of(UUID.fromString(id));
        } catch (final IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
