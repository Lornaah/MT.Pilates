package fr.mtpilates.shared.storage;

import java.io.InputStream;
import java.util.Optional;
import org.springframework.core.io.Resource;

/**
 * Stockage de fichiers (avatars, photos, documents…). Remplace les buckets
 * Supabase. Les règles d'accès ne sont pas ici : elles sont vérifiées par le
 * service métier avant tout appel.
 */
public interface StorageService {

    /** Enregistre le contenu sous la clé donnée (remplace l'existant) et renvoie ses métadonnées. */
    StoredFile store(StorageKey key, InputStream content, String contentType);

    /** Fichier associé à la clé, s'il existe. */
    Optional<Resource> load(StorageKey key);

    /** Supprime le fichier ; sans effet s'il n'existe pas. */
    void delete(StorageKey key);
}
