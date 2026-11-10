package fr.mtpilates.shared.storage;

/** Métadonnées d'un fichier enregistré. */
public record StoredFile(StorageKey key, long sizeBytes, String contentType) {
}
