package fr.mtpilates.document;

import fr.mtpilates.auth.CurrentUser;
import fr.mtpilates.auth.CurrentUserService;
import fr.mtpilates.member.MemberFilters;
import fr.mtpilates.member.MemberService;
import fr.mtpilates.shared.web.Toast;
import fr.mtpilates.shared.web.ToastResponses;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

/** Bibliothèque de documents (/app/coach/documents) et téléchargement autorisé (/app/documents/{id}). */
@Controller
public class DocumentController {

    private static final String GRID = "pages/documents :: grid";

    private final DocumentService documents;
    private final MemberService members;
    private final CurrentUserService currentUsers;
    private final ToastResponses toasts;

    public DocumentController(final DocumentService documents, final MemberService members,
                              final CurrentUserService currentUsers, final ToastResponses toasts) {
        this.documents = documents;
        this.members = members;
        this.currentUsers = currentUsers;
        this.toasts = toasts;
    }

    @GetMapping("/app/coach/documents")
    public String page(final Model model) {
        fill(model);
        return "pages/documents";
    }

    @GetMapping({"/app/coach/documents/new", "/app/coach/documents/{id}/edit"})
    public String formDialog(@PathVariable(required = false) final UUID id, final Model model) {
        final CurrentUser user = currentUsers.require();
        model.addAttribute("form", documents.formValues(user, Optional.ofNullable(id)));
        model.addAttribute("categories", DocumentCatalog.CATEGORIES);
        model.addAttribute("members", members.visibleTo(user, new MemberFilters("", MemberFilters.ALL, MemberFilters.ALL)));
        return "pages/documents :: documentDialog";
    }

    @PostMapping("/app/coach/documents/upload")
    public String upload(@RequestParam final MultipartFile file, @RequestParam(defaultValue = "") final String title,
                         final Model model) throws IOException {
        try (InputStream content = file.getInputStream()) {
            model.addAttribute("uploaded", documents.upload(currentUsers.require(), file.getOriginalFilename(),
                    file.getContentType(), file.getSize(), content));
        }
        model.addAttribute("currentTitle", title);
        return "pages/documents :: uploaded";
    }

    @PostMapping({"/app/coach/documents", "/app/coach/documents/{id}"})
    public String save(@PathVariable(required = false) final UUID id, @ModelAttribute final DocumentForm form,
                       final HttpServletResponse response, final Model model) {
        try {
            documents.save(currentUsers.require(), Optional.ofNullable(id), form);
            toasts.trigger(response, Toast.success(id == null ? "Document ajouté" : "Document mis à jour"),
                    Map.of("dialog-close", "document-form"));
        } catch (final IllegalArgumentException e) {
            toasts.trigger(response, Toast.error(e.getMessage()));
            response.setHeader("HX-Reswap", "none");
        }
        fill(model);
        return GRID;
    }

    @DeleteMapping("/app/coach/documents/{id}")
    public String delete(@PathVariable final UUID id, final HttpServletResponse response, final Model model) {
        documents.delete(currentUsers.require(), id);
        toasts.trigger(response, Toast.success("Document supprimé"));
        fill(model);
        return GRID;
    }

    @GetMapping("/app/documents/{id}")
    public ResponseEntity<Resource> download(@PathVariable final UUID id) {
        return documents.download(currentUsers.require(), id)
                .map(d -> ResponseEntity.ok()
                        .contentType(d.mime().map(MediaType::parseMediaType).orElse(MediaType.APPLICATION_OCTET_STREAM))
                        .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename(d.title(),
                                java.nio.charset.StandardCharsets.UTF_8).build().toString())
                        .body(d.resource()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private void fill(final Model model) {
        final var list = documents.readable(currentUsers.require());
        model.addAttribute("documents", list);
        model.addAttribute("allTags", list.stream().flatMap(d -> d.tags().stream()).distinct().sorted().toList());
        model.addAttribute("categories", DocumentCatalog.CATEGORIES);
    }
}
