package fr.mtpilates.member;

import fr.mtpilates.auth.CurrentUser;
import fr.mtpilates.auth.CurrentUserService;
import fr.mtpilates.member.MemberDocumentService.DocumentUpload;
import fr.mtpilates.shared.web.Toast;
import fr.mtpilates.shared.web.ToastResponses;
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

/** Documents d'un membre ; chaque action renvoie la carte à jour. */
@Controller
public class MemberDocumentsController {

    private static final String CARD = "fragments/member/documents :: card(memberId=${memberId}, documents=${documents})";

    private final MemberDocumentService documents;
    private final CurrentUserService currentUsers;
    private final ToastResponses toasts;

    public MemberDocumentsController(final MemberDocumentService documents, final CurrentUserService currentUsers,
                                     final ToastResponses toasts) {
        this.documents = documents;
        this.currentUsers = currentUsers;
        this.toasts = toasts;
    }

    @PostMapping("/app/members/{memberId}/documents")
    public String upload(@PathVariable final UUID memberId, @RequestParam(required = false) final MultipartFile file,
                         @RequestParam(defaultValue = "") final String title,
                         @RequestParam(defaultValue = "") final String category,
                         final HttpServletResponse response, final Model model) throws IOException {
        final CurrentUser user = currentUsers.require();
        if (file == null || file.isEmpty()) {
            response.setHeader("HX-Reswap", "none");
            return card(model, user, memberId);
        }
        final boolean uploaded;
        try (InputStream content = file.getInputStream()) {
            uploaded = documents.upload(user, memberId,
                    new DocumentUpload(file.getOriginalFilename(), file.getContentType(), file.getSize(), title, category),
                    content);
        }
        if (uploaded) {
            toasts.trigger(response, Toast.success("Document ajouté"), Map.of("dialog-close", "document-upload"));
        } else {
            response.setHeader("HX-Reswap", "none");
        }
        return card(model, user, memberId);
    }

    @DeleteMapping("/app/members/documents/{documentId}")
    public String delete(@PathVariable final UUID documentId, @RequestParam final UUID memberId,
                         final HttpServletResponse response, final Model model) {
        final CurrentUser user = currentUsers.require();
        documents.delete(user, documentId);
        toasts.trigger(response, Toast.success("Document supprimé"));
        return card(model, user, memberId);
    }

    private String card(final Model model, final CurrentUser user, final UUID memberId) {
        model.addAttribute("memberId", memberId);
        model.addAttribute("documents", documents.documents(user, memberId));
        return CARD;
    }
}
