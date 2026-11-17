package fr.mtpilates.video;

import fr.mtpilates.auth.CurrentUserService;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

/** « Vos séances vidéo » (/app/videos) : filtres côté navigateur, lecteur en dialog, visionnage enregistré. */
@Controller
public class VideoLibraryController {

    private final VideoService videos;
    private final CurrentUserService currentUsers;

    public VideoLibraryController(final VideoService videos, final CurrentUserService currentUsers) {
        this.videos = videos;
        this.currentUsers = currentUsers;
    }

    @GetMapping("/app/videos")
    public String library(final Model model) {
        model.addAttribute("videos", videos.published());
        model.addAttribute("levels", VideoLevel.values());
        return "pages/videos";
    }

    @PostMapping("/app/videos/{id}/views")
    public ResponseEntity<Void> recordView(@PathVariable final UUID id) {
        videos.recordView(currentUsers.require(), id);
        return ResponseEntity.noContent().build();
    }
}
