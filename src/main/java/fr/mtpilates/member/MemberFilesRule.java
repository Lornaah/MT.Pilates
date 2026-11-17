package fr.mtpilates.member;

import fr.mtpilates.auth.CurrentUser;
import fr.mtpilates.shared.files.FileAccessRule;
import fr.mtpilates.shared.storage.StorageKey;
import java.util.Optional;
import java.util.UUID;

/**
 * Buckets privés rangés par membre ({@code <membre>/<fichier>}) : lisibles par le membre et par qui le gère,
 * comme les policies storage de l'original.
 */
public class MemberFilesRule implements FileAccessRule {

    private final String bucket;
    private final MemberGuard guard;

    public MemberFilesRule(final String bucket, final MemberGuard guard) {
        this.bucket = bucket;
        this.guard = guard;
    }

    @Override
    public String bucket() {
        return bucket;
    }

    @Override
    public boolean canRead(final Optional<CurrentUser> user, final StorageKey key) {
        return user.isPresent() && ownerOf(key).map(memberId -> guard.canManage(user.get(), memberId)).orElse(false);
    }

    private static Optional<UUID> ownerOf(final StorageKey key) {
        final String folder = key.path().split("/", 2)[0];
        try {
            return Optional.of(UUID.fromString(folder));
        } catch (final IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
