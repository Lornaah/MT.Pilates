package fr.mtpilates.video;

import fr.mtpilates.auth.CurrentUserService;
import fr.mtpilates.shared.web.Toast;
import fr.mtpilates.shared.web.ToastResponses;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/** Gestion de la bibliothèque (/app/coach/videos). */
@Controller
@RequestMapping("/app/coach/videos")
public class VideoManagerController {

    private static final String GRID = "pages/video-manager :: grid";

    private final VideoService videos;
    private final CurrentUserService currentUsers;
    private final ToastResponses toasts;

    public VideoManagerController(final VideoService videos, final CurrentUserService currentUsers,
                                  final ToastResponses toasts) {
        this.videos = videos;
        this.currentUsers = currentUsers;
        this.toasts = toasts;
    }

    @GetMapping
    public String page(final Model model) {
        model.addAttribute("videos", videos.all(currentUsers.require()));
        model.addAttribute("levels", VideoLevel.values());
        return "pages/video-manager";
    }

    @PostMapping
    public String create(@ModelAttribute final VideoForm form, final HttpServletResponse response, final Model model) {
        try {
            videos.create(currentUsers.require(), form);
            toasts.trigger(response, Toast.success("Séance ajoutée"), Map.of("dialog-close", "video-create"));
        } catch (final IllegalArgumentException e) {
            toasts.trigger(response, Toast.error(e.getMessage()));
            response.setHeader("HX-Reswap", "none");
        }
        model.addAttribute("videos", videos.all(currentUsers.require()));
        return GRID;
    }

    @DeleteMapping("/{id}")
    public String delete(@PathVariable final UUID id, final HttpServletResponse response, final Model model) {
        videos.delete(currentUsers.require(), id);
        toasts.trigger(response, Toast.success("Supprimée"));
        model.addAttribute("videos", videos.all(currentUsers.require()));
        return GRID;
    }
}
