package fr.mtpilates.document;

import java.util.List;
import java.util.UUID;

/** Contenu d'un document de la bibliothèque. */
public record DocumentContent(String title, String description, String category, List<String> tags, String visibility,
                              List<UUID> sharedWithIds, String fileUrl, Integer fileSizeBytes, String mimeType) {
}
