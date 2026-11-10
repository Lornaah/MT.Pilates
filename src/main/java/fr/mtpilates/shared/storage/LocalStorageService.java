package fr.mtpilates.shared.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

/** Stockage sur le disque local, sous {@code app.storage.root}/{bucket}/{chemin}. */
@Service
public class LocalStorageService implements StorageService {

    private final Path root;

    public LocalStorageService(final StorageProperties properties) {
        this.root = properties.root().toAbsolutePath().normalize();
    }

    @Override
    public StoredFile store(final StorageKey key, final InputStream content, final String contentType) {
        final Path target = resolve(key);
        try {
            Files.createDirectories(target.getParent());
            final long size = Files.copy(content, target, StandardCopyOption.REPLACE_EXISTING);
            return new StoredFile(key, size, contentType);
        } catch (final IOException e) {
            throw new StorageException("Écriture impossible : " + key, e);
        }
    }

    @Override
    public Optional<Resource> load(final StorageKey key) {
        final Path file = resolve(key);
        return Files.isRegularFile(file) ? Optional.of(new PathResource(file)) : Optional.empty();
    }

    @Override
    public void delete(final StorageKey key) {
        try {
            Files.deleteIfExists(resolve(key));
        } catch (final IOException e) {
            throw new StorageException("Suppression impossible : " + key, e);
        }
    }

    private Path resolve(final StorageKey key) {
        final Path path = root.resolve(key.bucket()).resolve(key.path()).normalize();
        if (!path.startsWith(root)) {
            throw new IllegalArgumentException("Chemin hors du stockage : " + key);
        }
        return path;
    }
}
