package fr.mtpilates.member;

import fr.mtpilates.auth.CurrentUser;
import fr.mtpilates.shared.storage.StorageKey;
import fr.mtpilates.shared.storage.StorageService;
import fr.mtpilates.shared.ui.FrenchDates;
import java.io.InputStream;
import java.time.Clock;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Documents d'un membre : déposés et consultés par le membre et par qui le suit. */
@Service
public class MemberDocumentService {

    private static final String URL_PREFIX = "/files/" + MemberBuckets.DOCUMENTS + "/";
    private static final long KILO = 1024;

    private final MemberDocumentRepository documents;
    private final StorageService storage;
    private final MemberGuard guard;
    private final Clock clock;

    public MemberDocumentService(final MemberDocumentRepository documents, final StorageService storage,
                                 final MemberGuard guard, final Clock clock) {
        this.documents = documents;
        this.storage = storage;
        this.guard = guard;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public MemberDocuments documents(final CurrentUser user, final UUID memberId) {
        guard.requireManage(user, memberId);
        final ZoneId zone = clock.getZone();
        return MemberDocuments.of(documents.findByUserIdOrderByUploadedAtDesc(memberId).stream()
                .map(d -> new MemberDocumentView(d.getId(), d.getTitle(), d.getCategory(),
                        FrenchDates.medium(d.getUploadedAt().atZone(zone).toLocalDate()),
                        formatBytes(d.getFileSizeBytes()), d.getFileUrl()))
                .toList());
    }

    /** Sans titre ou sans fichier, rien n'est envoyé (l'original ignorait le clic). */
    @Transactional
    public boolean upload(final CurrentUser user, final UUID memberId, final DocumentUpload upload,
                          final InputStream content) {
        guard.requireManage(user, memberId);
        final String title = Optional.ofNullable(upload.title()).map(String::trim).orElse("");
        if (title.isEmpty() || upload.size() == 0) {
            return false;
        }
        final String safe = Optional.ofNullable(upload.filename()).orElse("fichier").replaceAll("[^\\w.\\-]+", "_");
        final String path = memberId + "/" + clock.millis() + "-" + safe;
        storage.store(new StorageKey(MemberBuckets.DOCUMENTS, path), content, upload.contentType());
        final String category = Optional.ofNullable(upload.category()).map(String::trim).filter(c -> !c.isEmpty())
                .orElse(null);
        documents.save(new MemberDocument(memberId, user.id(), title, category, URL_PREFIX + path, upload.size(),
                Optional.ofNullable(upload.contentType()).filter(t -> !t.isEmpty()).orElse(null), clock.instant()));
        return true;
    }

    @Transactional
    public void delete(final CurrentUser user, final UUID documentId) {
        final MemberDocument document = documents.findById(documentId)
                .orElseThrow(() -> new AccessDeniedException("Document introuvable"));
        guard.requireManage(user, document.getUserId());
        documents.delete(document);
        if (document.getFileUrl().startsWith(URL_PREFIX)) {
            storage.delete(new StorageKey(MemberBuckets.DOCUMENTS, document.getFileUrl().substring(URL_PREFIX.length())));
        }
    }

    public record DocumentUpload(String filename, String contentType, long size, String title, String category) {
    }

    /** formatBytes de l'original : « 512 o », « 1.5 Ko », « 2.3 Mo », « — » si inconnu. */
    static String formatBytes(final Optional<Long> bytes) {
        final long n = bytes.orElse(0L);
        if (n == 0) {
            return "—";
        }
        if (n < KILO) {
            return n + " o";
        }
        if (n < KILO * KILO) {
            return String.format(Locale.ROOT, "%.1f Ko", n / (double) KILO);
        }
        return String.format(Locale.ROOT, "%.1f Mo", n / (double) (KILO * KILO));
    }
}
