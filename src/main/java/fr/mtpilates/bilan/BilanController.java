package fr.mtpilates.bilan;

import fr.mtpilates.auth.CurrentUser;
import fr.mtpilates.auth.CurrentUserService;
import fr.mtpilates.shared.web.Flash;
import fr.mtpilates.shared.web.Toast;
import fr.mtpilates.shared.web.ToastResponses;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.UUID;
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
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/** Bilans : création du brouillon, éditeur du coach, aperçu, envoi, suppression et téléchargement. */
@Controller
public class BilanController {

    private static final String EDITOR = "/app/coach/bilans/";

    private final BilanService bilans;
    private final BilanTemplateService templates;
    private final CurrentUserService currentUsers;
    private final ToastResponses toasts;

    public BilanController(final BilanService bilans, final BilanTemplateService templates,
                           final CurrentUserService currentUsers, final ToastResponses toasts) {
        this.bilans = bilans;
        this.templates = templates;
        this.currentUsers = currentUsers;
        this.toasts = toasts;
    }

    @PostMapping("/app/coach/members/{memberId}/bilans")
    public ResponseEntity<Void> create(@PathVariable final UUID memberId) {
        final UUID id = bilans.createDraft(currentUsers.require(), memberId);
        return ResponseEntity.noContent().header("HX-Redirect", EDITOR + id).build();
    }

    @GetMapping(EDITOR + "{id}")
    public String editor(@PathVariable final UUID id, final Model model) {
        model.addAttribute("bilan", bilans.editor(currentUsers.require(), id));
        model.addAttribute("sections", BilanTemplateCatalog.SECTIONS);
        model.addAttribute("variables", BilanTemplateCatalog.VARIABLES);
        model.addAttribute("templateTexts", templates.current());
        return "pages/bilan-editor";
    }

    @PostMapping(EDITOR + "{id}")
    public ResponseEntity<Void> save(@PathVariable final UUID id, @ModelAttribute final BilanSettingsForm form,
                                     final HttpServletResponse response) {
        try {
            final boolean sent = bilans.save(currentUsers.require(), id, form);
            toasts.trigger(response, Toast.success(sent ? "Modifications enregistrées" : "Brouillon enregistré"),
                    Map.of("bilan-saved", true));
        } catch (final IllegalArgumentException e) {
            toasts.trigger(response, Toast.error(e.getMessage()));
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping(EDITOR + "{id}/pdf")
    public ResponseEntity<byte[]> preview(@PathVariable final UUID id) {
        return pdf(bilans.preview(currentUsers.require(), id), "apercu-bilan.pdf", true);
    }

    /** Enregistre les derniers réglages puis envoie : retour sur la fiche membre, onglet des bilans. */
    @PostMapping(EDITOR + "{id}/send")
    public ResponseEntity<Void> send(@PathVariable final UUID id, @ModelAttribute final BilanSettingsForm form,
                                     @RequestParam(defaultValue = "") final String notify,
                                     final HttpServletRequest request, final HttpServletResponse response) {
        final CurrentUser user = currentUsers.require();
        try {
            bilans.save(user, id, form);
            final String link = ServletUriComponentsBuilder.fromContextPath(request).path("/app/tracking").toUriString();
            final BilanService.SendResult result = bilans.send(user, id, link, notify.contains("true"));
            final String target = "/app/coach/members/" + bilans.editor(user, id).memberId() + "?bilans";
            Flash.put(request, response, target, Map.of(ToastResponses.FLASH_ATTRIBUTE, toast(result)));
            return ResponseEntity.noContent().header("HX-Redirect", target).build();
        } catch (final IllegalArgumentException e) {
            toasts.trigger(response, Toast.error(e.getMessage()));
            return ResponseEntity.noContent().build();
        }
    }

    private static Toast toast(final BilanService.SendResult result) {
        return switch (result) {
            case MAILED -> Toast.success("Bilan envoyé à la membre");
            case UPDATED -> Toast.success("Bilan mis à jour dans l'espace de la membre");
            case MAIL_FAILED -> Toast.error("Bilan disponible dans l'espace de la membre, mais l'email n'a pas pu partir");
        };
    }

    @DeleteMapping(EDITOR + "{id}")
    public String delete(@PathVariable final UUID id, final HttpServletResponse response, final Model model) {
        final CurrentUser user = currentUsers.require();
        final UUID memberId = bilans.delete(user, id);
        toasts.trigger(response, Toast.success("Bilan supprimé"));
        model.addAttribute("bilans", bilans.forMember(user, memberId));
        model.addAttribute("memberId", memberId);
        return "fragments/bilan/list :: list";
    }

    @GetMapping("/app/bilans/{id}/pdf")
    public ResponseEntity<byte[]> download(@PathVariable final UUID id) {
        return bilans.download(currentUsers.require(), id).map(d -> pdf(d.pdf(), d.filename(), false))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private static ResponseEntity<byte[]> pdf(final byte[] body, final String filename, final boolean inline) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, (inline ? ContentDisposition.inline() : ContentDisposition.attachment())
                        .filename(filename).build().toString())
                .body(body);
    }
}
