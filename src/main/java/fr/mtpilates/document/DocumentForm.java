package fr.mtpilates.document;

import java.util.List;

/** Formulaire « Nouveau document » / « Modifier le document » (le fichier est téléversé à part). */
public record DocumentForm(String title, String description, String category, List<String> tags, String visibility,
                           List<String> sharedWithIds, String fileUrl, String fileSize, String mimeType) {
}
