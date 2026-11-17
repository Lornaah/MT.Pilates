package fr.mtpilates.tracking;

import fr.mtpilates.auth.CurrentUser;
import fr.mtpilates.auth.CurrentUserService;
import fr.mtpilates.shared.web.Toast;
import fr.mtpilates.shared.web.ToastResponses;
import fr.mtpilates.tracking.MemberPhotoService.PhotoUpload;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

/** Photos d'évolution d'un membre ; chaque action renvoie la carte à jour. */
@Controller
public class MemberPhotosController {

    private static final String CARD = "fragments/tracking/photos :: card(memberId=${memberId}, photos=${photos})";

    private final MemberPhotoService photos;
    private final CurrentUserService currentUsers;
    private final ToastResponses toasts;

    public MemberPhotosController(final MemberPhotoService photos, final CurrentUserService currentUsers,
                                  final ToastResponses toasts) {
        this.photos = photos;
        this.currentUsers = currentUsers;
        this.toasts = toasts;
    }

    @PostMapping("/app/members/{memberId}/photos")
    public String upload(@PathVariable final UUID memberId, @RequestParam final MultipartFile file,
                         @RequestParam(defaultValue = "front") final String side,
                         @RequestParam(defaultValue = "") final String takenAt,
                         @RequestParam(defaultValue = "") final String notes,
                         final HttpServletResponse response, final Model model) throws IOException {
        final CurrentUser user = currentUsers.require();
        try (InputStream content = file.getInputStream()) {
            photos.upload(user, memberId, new PhotoUpload(file.getOriginalFilename(), file.getContentType(), side, takenAt,
                    notes), content);
        }
        toasts.trigger(response, Toast.success("Photo ajoutée"), Map.of("dialog-close", "photo-upload"));
        return card(model, user, memberId);
    }

    @DeleteMapping("/app/members/photos/{photoId}")
    public String delete(@PathVariable final UUID photoId, @RequestParam final UUID memberId,
                         final HttpServletResponse response, final Model model) {
        final CurrentUser user = currentUsers.require();
        photos.delete(user, photoId);
        toasts.trigger(response, Toast.success("Photo supprimée"));
        return card(model, user, memberId);
    }

    private String card(final Model model, final CurrentUser user, final UUID memberId) {
        model.addAttribute("memberId", memberId);
        model.addAttribute("photos", photos.photos(user, memberId));
        return CARD;
    }
}
