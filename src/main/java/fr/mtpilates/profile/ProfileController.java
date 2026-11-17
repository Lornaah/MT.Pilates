package fr.mtpilates.profile;

import fr.mtpilates.auth.CurrentUser;
import fr.mtpilates.auth.CurrentUserService;
import fr.mtpilates.document.DocumentService;
import fr.mtpilates.member.MemberDocumentService;
import fr.mtpilates.shared.web.Toast;
import fr.mtpilates.shared.web.ToastResponses;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

/** « Ma fiche » (/app/profile). */
@Controller
@RequestMapping("/app/profile")
public class ProfileController {

    private final ProfileService profiles;
    private final MemberDocumentService documents;
    private final DocumentService sharedDocuments;
    private final CurrentUserService currentUsers;
    private final ToastResponses toasts;

    public ProfileController(final ProfileService profiles, final MemberDocumentService documents,
                             final DocumentService sharedDocuments, final CurrentUserService currentUsers, final ToastResponses toasts) {
        this.profiles = profiles;
        this.documents = documents;
        this.sharedDocuments = sharedDocuments;
        this.currentUsers = currentUsers;
        this.toasts = toasts;
    }

    @GetMapping
    public String page(final Model model) {
        final CurrentUser user = currentUsers.require();
        model.addAttribute("profile", profiles.own(user));
        model.addAttribute("memberId", user.id());
        model.addAttribute("documents", documents.documents(user, user.id()));
        model.addAttribute("sharedDocuments", sharedDocuments.shared(user));
        return "pages/profile";
    }

    @PostMapping
    public ResponseEntity<Void> save(@ModelAttribute final ProfileForm form, final HttpServletResponse response) {
        profiles.updateOwn(currentUsers.require(), form);
        toasts.trigger(response, Toast.success("Fiche mise à jour"));
        return ResponseEntity.noContent().header("HX-Reswap", "none").build();
    }

    /** Nouvelle photo : la carte d'identité est re-rendue avec l'avatar à jour. */
    @PostMapping("/avatar")
    public String avatar(@RequestParam final MultipartFile file, final HttpServletResponse response, final Model model)
            throws IOException {
        try (var content = file.getInputStream()) {
            profiles.changeAvatar(currentUsers.require(), file.getOriginalFilename(), file.getContentType(), content);
        }
        toasts.trigger(response, Toast.success("Photo mise à jour"));
        model.addAttribute("profile", profiles.own(currentUsers.require()));
        return "pages/profile :: avatar";
    }
}
