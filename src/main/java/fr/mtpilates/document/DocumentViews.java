package fr.mtpilates.document;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Vues de la bibliothèque de documents. */
public final class DocumentViews {

    private DocumentViews() {
    }

    public record Item(UUID id, String title, Optional<String> description, String category, String categoryLabel,
                       String visibility, String visibilityLabel, String visibilityIcon, String sizeLabel, String icon,
                       List<String> tags) {
    }

    public record Uploaded(String path, int size, String mime, String suggestedTitle) {

        public String sizeLabel() {
            return DocumentCatalog.size(size);
        }
    }

    public record FormValues(Optional<UUID> id, String title, String description, String category, List<String> tags,
                             String visibility, List<UUID> sharedWithIds, String fileUrl, Integer fileSize, String mime) {

        public String sizeLabel() {
            return DocumentCatalog.size(fileSize);
        }
    }
}
