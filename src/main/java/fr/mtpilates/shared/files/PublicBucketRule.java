package fr.mtpilates.shared.files;

import fr.mtpilates.auth.CurrentUser;
import fr.mtpilates.shared.storage.StorageKey;
import java.util.Optional;

/** Bucket public de l'original (avatars, images d'annonces) : lisible par tous. */
public record PublicBucketRule(String bucket) implements FileAccessRule {

    @Override
    public boolean canRead(final Optional<CurrentUser> user, final StorageKey key) {
        return true;
    }
}
