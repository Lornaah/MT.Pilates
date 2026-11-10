package fr.mtpilates.shared.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;

class LocalStorageServiceTest {

    @TempDir
    private Path root;

    private LocalStorageService storage;

    @BeforeEach
    void setUp() {
        storage = new LocalStorageService(new StorageProperties(root));
    }

    @Test
    void storesThenLoadsAFile() throws IOException {
        final StorageKey key = new StorageKey("member-photos", "user-1/1700000000-front.jpg");

        final StoredFile stored = storage.store(key, bytes("photo"), "image/jpeg");
        final Resource loaded = storage.load(key).orElseThrow();

        assertThat(stored.sizeBytes()).isEqualTo(5);
        assertThat(loaded.getContentAsString(StandardCharsets.UTF_8)).isEqualTo("photo");
    }

    @Test
    void replacesAnExistingFile() throws IOException {
        final StorageKey key = new StorageKey("avatars", "user-1/avatar.png");
        storage.store(key, bytes("old"), "image/png");

        storage.store(key, bytes("new"), "image/png");

        assertThat(storage.load(key).orElseThrow().getContentAsString(StandardCharsets.UTF_8)).isEqualTo("new");
    }

    @Test
    void deletesAFileAndIgnoresMissingOnes() {
        final StorageKey key = new StorageKey("documents", "doc.pdf");
        storage.store(key, bytes("pdf"), "application/pdf");

        storage.delete(key);
        storage.delete(key);

        assertThat(storage.load(key)).isEmpty();
    }

    @Test
    void refusesKeysThatEscapeTheBucket() {
        assertThatThrownBy(() -> new StorageKey("documents", "../secret.txt"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new StorageKey("documents", "a//b.txt"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new StorageKey("../etc", "passwd"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static ByteArrayInputStream bytes(final String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }
}
