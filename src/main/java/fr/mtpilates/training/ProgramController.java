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
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Planifications (/app/coach/programs) et leur éditeur. */
@Controller
@RequestMapping("/app/coach/programs")
public class ProgramController {

    private static final String EDITOR_PATH = "/app/coach/programs/";
    private static final String PREFILL = "prefill";

    private final ProgramService programs;
    private final CurrentUserService currentUsers;
    private final ToastResponses toasts;
    private final JsonWriter json;

    public ProgramController(final ProgramService programs, final CurrentUserService currentUsers,
                             final ToastResponses toasts, final JsonWriter json) {
        this.programs = programs;
        this.currentUsers = currentUsers;
        this.toasts = toasts;
        this.json = json;
    }

    @GetMapping
    public String library(final Model model) {
        model.addAttribute("programs", programs.library(currentUsers.require()));
        model.addAttribute("statuses", ProgramStatus.values());
        return "pages/programs";
    }

    @GetMapping("/new")
    public String create(@RequestParam(required = false) final UUID assignee,
                         @ModelAttribute(PREFILL) final Object prefill, final Model model) {
        final CurrentUser user = currentUsers.require();
        final Object state = prefill instanceof Map<?, ?> map && !map.isEmpty()
                ? map : programs.editorState(user, Optional.empty(), Optional.ofNullable(assignee));
        return editor(model, user, Optional.empty(), state);
    }

    @GetMapping("/{id}")
    public String edit(@PathVariable final UUID id, final Model model) {
        final CurrentUser user = currentUsers.require();
        return editor(model, user, Optional.of(id), programs.editorState(user, Optional.of(id), Optional.empty()));
    }

    /** Pré-remplissage : la copie n'est pas enregistrée, elle est seulement chargée dans un nouvel éditeur. */
    @GetMapping("/prefill/{sourceId}")
    public String prefill(@PathVariable final UUID sourceId, final RedirectAttributes redirect) {
        programs.prefill(currentUsers.require(), sourceId).ifPresent(state -> {
            redirect.addFlashAttribute(PREFILL, state);
            toasts.flash(redirect, Toast.success("Modèle chargé — modifie puis enregistre"));
        });
        return "redirect:/app/coach/programs/new";
    }

    @PostMapping("/save")
    public ResponseEntity<Void> save(@RequestParam(defaultValue = "") final String id, @RequestParam final String payload,
                                     @RequestParam(defaultValue = "draft") final String status,
                                     final HttpServletRequest request, final HttpServletResponse response) {
        try {
            final UUID saved = programs.save(currentUsers.require(), parse(id), payload, status);
            flash(request, response, saved, "Planification enregistrée");
            return ResponseEntity.noContent().header("HX-Redirect", EDITOR_PATH + saved).build();
        } catch (final SessionValidationException e) {
            toasts.trigger(response, Toast.error(e.getMessage()));
            return ResponseEntity.noContent().build();
        }
    }

    @PostMapping("/{id}/duplicate")
    public ResponseEntity<Void> duplicate(@PathVariable final UUID id, final HttpServletRequest request,
                                          final HttpServletResponse response) {
        final UUID copy = programs.duplicate(currentUsers.require(), id);
        flash(request, response, copy, "Programme dupliqué");
        return ResponseEntity.noContent().header("HX-Redirect", EDITOR_PATH + copy).build();
    }

    @DeleteMapping("/{id}")
    public String delete(@PathVariable final UUID id, final HttpServletResponse response, final Model model) {
        final CurrentUser user = currentUsers.require();
        programs.delete(user, id);
        toasts.trigger(response, Toast.success("Planification supprimée"));
        model.addAttribute("programs", programs.library(user));
        return "fragments/training/programs :: grid";
    }

    private String editor(final Model model, final CurrentUser user, final Optional<UUID> id, final Object state) {
        model.addAttribute("programId", id.map(UUID::toString).orElse(""));
        model.addAttribute("isNew", id.isEmpty());
        model.addAttribute("stateJson", json.write(state));
        model.addAttribute("startDate", state instanceof Map<?, ?> map && map.get("start_date") != null
                ? String.valueOf(map.get("start_date")) : "");
        model.addAttribute("membersJson", json.write(programs.memberOptions(user)));
        model.addAttribute("sessionsJson", json.write(programs.sessionOptions()));
        model.addAttribute("programOptions", programs.programOptions());
        return "pages/program-editor";
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
