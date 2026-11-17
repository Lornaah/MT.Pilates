package fr.mtpilates.finance;

import fr.mtpilates.auth.CurrentUser;
import fr.mtpilates.auth.CurrentUserService;
import fr.mtpilates.member.MemberDocumentService;
import fr.mtpilates.member.MemberDocumentView;
import fr.mtpilates.shared.web.Toast;
import fr.mtpilates.shared.web.ToastResponses;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import org.springframework.web.multipart.MultipartFile;

/** Finance (/app/money) : la page est re-rendue par le serveur à chaque changement de filtre ou de tri. */
@Controller
public class FinanceController {

    private static final String VIEW = "pages/money";
    private static final String INVOICE_CATEGORY = "Facture";

    private final FinanceService finance;
    private final MemberDocumentService documents;
    private final CurrentUserService currentUsers;
    private final ToastResponses toasts;
    private final Clock clock;

    public FinanceController(final FinanceService finance, final MemberDocumentService documents,
                             final CurrentUserService currentUsers, final ToastResponses toasts, final Clock clock) {
        this.finance = finance;
        this.documents = documents;
        this.currentUsers = currentUsers;
        this.toasts = toasts;
        this.clock = clock;
    }

    @GetMapping("/app/money")
    public String page(@ModelAttribute final FinanceFilters filters, final Model model) {
        model.addAttribute("page", finance.page(currentUsers.require(), filters));
        model.addAttribute("filters", filters);
        return VIEW;
    }

    @GetMapping("/app/admin/finance")
    public String legacyRedirect() {
        return "redirect:/app/money";
    }

    @GetMapping("/app/money/content")
    public String content(@ModelAttribute final FinanceFilters filters, final Model model) {
        page(filters, model);
        return VIEW + " :: content";
    }

    @GetMapping("/app/money/export")
    public ResponseEntity<byte[]> export(@ModelAttribute final FinanceFilters filters) {
        final byte[] body = ("﻿" + finance.csv(currentUsers.require(), filters)).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("finance-" + LocalDate.now(clock) + ".csv").build().toString())
                .body(body);
    }

    @GetMapping("/app/money/{id}/edit")
    public String editDialog(@PathVariable final UUID id, final Model model) {
        model.addAttribute("edit", finance.editValues(currentUsers.require(), id));
        return VIEW + " :: editDialog";
    }

    @PostMapping("/app/money/{id}")
    public String update(@PathVariable final UUID id, @ModelAttribute final BillingForm form,
                         @ModelAttribute final FinanceFilters filters, final HttpServletResponse response, final Model model) {
        finance.update(currentUsers.require(), id, form);
        toasts.trigger(response, Toast.success("Fiche mise à jour"), Map.of("dialog-close", "finance-edit"));
        return content(filters, model);
    }

    @GetMapping("/app/money/{id}/reminder")
    public String reminderDialog(@PathVariable final UUID id, @ModelAttribute final FinanceFilters filters, final Model model) {
        final CurrentUser user = currentUsers.require();
        model.addAttribute("row", finance.page(user, new FinanceFilters(null, null, null, null, null, null)).rows().stream()
                .filter(r -> r.id().equals(id)).findFirst().orElseThrow());
        return VIEW + " :: reminderDialog";
    }

    @GetMapping("/app/money/{id}/invoices")
    public String invoices(@PathVariable final UUID id, final Model model) {
        fillInvoices(id, model);
        return VIEW + " :: invoicesDialog";
    }

    @PostMapping("/app/money/{id}/invoices")
    public String uploadInvoice(@PathVariable final UUID id, @RequestParam(defaultValue = "") final String title,
                                @RequestParam(required = false) final MultipartFile file, final HttpServletResponse response,
                                final Model model) throws IOException {
        if (file != null && !file.isEmpty()) {
            final String finalTitle = title.trim().isEmpty() ? Optional.ofNullable(file.getOriginalFilename()).orElse("") : title.trim();
            try (InputStream content = file.getInputStream()) {
                documents.upload(currentUsers.require(), id, new MemberDocumentService.DocumentUpload(file.getOriginalFilename(),
                        file.getContentType(), file.getSize(), finalTitle, INVOICE_CATEGORY), content);
            }
            toasts.trigger(response, Toast.success("Document ajouté"));
        }
        fillInvoices(id, model);
        return VIEW + " :: invoicesBody";
    }

    @DeleteMapping("/app/money/{id}/invoices/{documentId}")
    public String deleteInvoice(@PathVariable final UUID id, @PathVariable final UUID documentId,
                                final HttpServletResponse response, final Model model) {
        documents.delete(currentUsers.require(), documentId);
        toasts.trigger(response, Toast.success("Document supprimé"));
        fillInvoices(id, model);
        return VIEW + " :: invoicesBody";
    }

    private void fillInvoices(final UUID memberId, final Model model) {
        final CurrentUser user = currentUsers.require();
        final List<MemberDocumentView> invoices = documents.documents(user, memberId).items().stream()
                .filter(d -> d.category().filter(INVOICE_CATEGORY::equals).isPresent()).toList();
        model.addAttribute("memberId", memberId);
        model.addAttribute("memberName", finance.memberName(user, memberId));
        model.addAttribute("invoices", invoices);
    }
}
