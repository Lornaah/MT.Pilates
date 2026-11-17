package fr.mtpilates.shared.files;

import fr.mtpilates.auth.CurrentUser;
import fr.mtpilates.shared.storage.StorageKey;
import java.util.Optional;

/** Règle de lecture d'un bucket (ex-policies storage de Supabase). Un bucket sans règle n'est pas servi. */
public interface FileAccessRule {

    String bucket();

    boolean canRead(Optional<CurrentUser> user, StorageKey key);
}
