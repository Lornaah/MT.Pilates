package fr.mtpilates.shared.files;

import fr.mtpilates.auth.CurrentUserService;
import fr.mtpilates.shared.storage.StorageKey;
import fr.mtpilates.shared.storage.StorageService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sert les fichiers stockés : /files/{bucket}/{chemin}. La lecture est soumise à la règle du bucket ;
 * un fichier interdit répond 404 pour ne pas révéler son existence.
 */
@RestController
public class FileController {

    private static final String PREFIX = "/files/";

    private final StorageService storage;
    private final CurrentUserService currentUsers;
    private final Map<String, FileAccessRule> rules;

    public FileController(final StorageService storage, final CurrentUserService currentUsers,
                          final List<FileAccessRule> rules) {
        this.storage = storage;
        this.currentUsers = currentUsers;
        this.rules = rules.stream().collect(Collectors.toMap(FileAccessRule::bucket, Function.identity()));
    }

    @GetMapping("/files/{bucket}/**")
    public ResponseEntity<Resource> read(@PathVariable final String bucket, final HttpServletRequest request) {
        final String path = request.getRequestURI().substring(request.getContextPath().length() + PREFIX.length() + bucket.length() + 1);
        final Optional<StorageKey> key = parse(bucket, path);
        return key
                .filter(k -> rules.containsKey(bucket) && rules.get(bucket).canRead(currentUsers.current(), k))
                .flatMap(storage::load)
                .map(resource -> ResponseEntity.ok()
                        .contentType(MediaTypeFactory.getMediaType(resource).orElse(MediaType.APPLICATION_OCTET_STREAM))
                        .body(resource))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    private static Optional<StorageKey> parse(final String bucket, final String path) {
        try {
            return Optional.of(new StorageKey(bucket, path));
        } catch (final IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
