package fr.mtpilates.document;

import fr.mtpilates.auth.CurrentUser;
import fr.mtpilates.auth.DataAccessRoles;
import fr.mtpilates.document.DocumentViews.FormValues;
import fr.mtpilates.document.DocumentViews.Item;
import fr.mtpilates.document.DocumentViews.Uploaded;
import fr.mtpilates.shared.storage.StorageKey;
import fr.mtpilates.shared.storage.StorageService;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Bibliothèque de documents partagés : gérée par le staff, lue selon la visibilité de chaque document. */
@Service
public class DocumentService {

    static final String BUCKET = "documents";

    private final LibraryDocumentRepository documents;
    private final StorageService storage;

    public DocumentService(final LibraryDocumentRepository documents, final StorageService storage) {
        this.documents = documents;
        this.storage = storage;
    }

    /** Documents lisibles par l'utilisateur (tous pour le staff). */
    @Transactional(readOnly = true)
    public List<Item> readable(final CurrentUser user) {
        final boolean staff = isStaff(user);
        return documents.findAllByOrderByCreatedAtDesc().stream().filter(d -> d.readableBy(user.id(), staff))
                .map(DocumentService::item).toList();
    }

    /** SharedDocumentsCard : documents lisibles, hors « Équipe uniquement ». */
    @Transactional(readOnly = true)
    public List<Item> shared(final CurrentUser user) {
        return readable(user).stream().filter(i -> !"staff_only".equals(i.visibility())).toList();
    }

    @Transactional(readOnly = true)
    public FormValues formValues(final CurrentUser user, final Optional<UUID> id) {
        requireStaff(user);
        return id.flatMap(documents::findById).map(d -> {
            final DocumentContent c = d.content();
            return new FormValues(Optional.of(d.getId()), c.title(), Optional.ofNullable(c.description()).orElse(""), c.category(),
                    c.tags(), c.visibility(), c.sharedWithIds(), c.fileUrl(), c.fileSizeBytes(), c.mimeType());
        }).orElseGet(() -> new FormValues(Optional.empty(), "", "", "guide", List.of(), "staff_only", List.of(), "", null, null));
    }

    /** Dépôt du fichier dans le bucket privé, sous un nom aléatoire ; le titre proposé est le nom sans extension. */
    public Uploaded upload(final CurrentUser user, final String filename, final String contentType, final long size,
                           final InputStream content) {
        requireStaff(user);
        final String name = Optional.ofNullable(filename).orElse("fichier");
        final int dot = name.lastIndexOf('.');
        final String ext = dot >= 0 && name.substring(dot + 1).matches("[A-Za-z0-9]{1,8}") ? name.substring(dot + 1) : "bin";
        final String path = UUID.randomUUID() + "." + ext;
        storage.store(new StorageKey(BUCKET, path), content, contentType);
        return new Uploaded(path, (int) Math.min(size, Integer.MAX_VALUE),
                Optional.ofNullable(contentType).filter(t -> !t.isBlank()).orElse("application/octet-stream"),
                dot > 0 ? name.substring(0, dot) : name);
    }

    @Transactional
    public void save(final CurrentUser user, final Optional<UUID> id, final DocumentForm form) {
        requireStaff(user);
        final String title = Optional.ofNullable(form.title()).map(String::trim).orElse("");
        final String file = Optional.ofNullable(form.fileUrl()).orElse("");
        if (title.isEmpty() || file.isEmpty()) {
            throw new IllegalArgumentException("Titre et fichier requis");
        }
        final String visibility = form.visibility() != null && DocumentCatalog.VISIBILITIES.containsKey(form.visibility())
                ? form.visibility() : "staff_only";
        final DocumentContent content = new DocumentContent(title,
                Optional.ofNullable(form.description()).map(String::trim).filter(s -> !s.isEmpty()).orElse(null),
                form.category() != null && DocumentCatalog.CATEGORIES.containsKey(form.category()) ? form.category() : "guide",
                Optional.ofNullable(form.tags()).orElse(List.of()), visibility,
                "specific".equals(visibility) ? uuids(form.sharedWithIds()) : List.of(), file, integer(form.fileSize()),
                Optional.ofNullable(form.mimeType()).filter(s -> !s.isBlank()).orElse(null));
        final LibraryDocument document = id.flatMap(documents::findById).orElseGet(() -> new LibraryDocument(user.id()));
        document.update(content, user.id());
        documents.save(document);
    }

    @Transactional
    public void delete(final CurrentUser user, final UUID id) {
        requireStaff(user);
        documents.findById(id).ifPresent(d -> {
            documents.delete(d);
            storage.delete(new StorageKey(BUCKET, d.content().fileUrl()));
        });
    }

    /** Téléchargement soumis à la visibilité (remplace les liens signés de l'original). */
    @Transactional(readOnly = true)
    public Optional<Download> download(final CurrentUser user, final UUID id) {
        return documents.findById(id).filter(d -> d.readableBy(user.id(), isStaff(user)))
                .flatMap(d -> storage.load(new StorageKey(BUCKET, d.content().fileUrl()))
                        .map(r -> new Download(r, d.content().title(), d.getMimeType())));
    }

    public record Download(Resource resource, String title, Optional<String> mime) {
    }

    private static Item item(final LibraryDocument d) {
        final DocumentContent c = d.content();
        return new Item(d.getId(), c.title(), Optional.ofNullable(c.description()).filter(s -> !s.isEmpty()), c.category(),
                DocumentCatalog.CATEGORIES.getOrDefault(c.category(), c.category()), c.visibility(),
                DocumentCatalog.VISIBILITIES.getOrDefault(c.visibility(), c.visibility()),
                DocumentCatalog.visibilityIcon(c.visibility()), DocumentCatalog.size(c.fileSizeBytes()),
                DocumentCatalog.icon(c.mimeType()), c.tags());
    }

    private static List<UUID> uuids(final List<String> values) {
        return Optional.ofNullable(values).orElse(List.of()).stream().flatMap(v -> {
            try {
                return java.util.stream.Stream.of(UUID.fromString(v));
            } catch (final IllegalArgumentException e) {
                return java.util.stream.Stream.empty();
            }
        }).toList();
    }

    private static Integer integer(final String value) {
        try {
            return value == null || value.isBlank() ? null : Integer.valueOf(value);
        } catch (final NumberFormatException e) {
            return null;
        }
    }

    private static boolean isStaff(final CurrentUser user) {
        return DataAccessRoles.isStaff(user.realRoles());
    }

    private static void requireStaff(final CurrentUser user) {
        if (!isStaff(user)) {
            throw new AccessDeniedException("Réservé au staff");
        }
    }
}
