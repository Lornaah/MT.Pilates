package fr.mtpilates.tracking;

import static fr.mtpilates.shared.web.FormValues.date;
import static fr.mtpilates.shared.web.FormValues.text;

import fr.mtpilates.auth.CurrentUser;
import fr.mtpilates.member.MemberBuckets;
import fr.mtpilates.member.MemberGuard;
import fr.mtpilates.shared.storage.StorageKey;
import fr.mtpilates.shared.storage.StorageService;
import fr.mtpilates.shared.ui.FrenchDates;
import fr.mtpilates.tracking.TrackingViews.PhotoView;
import java.io.InputStream;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Photos d'évolution : gérées par le membre et par qui le suit. */
@Service
public class MemberPhotoService {

    private static final String URL_PREFIX = "/files/" + MemberBuckets.PHOTOS + "/";

    private final MemberPhotoRepository photos;
    private final StorageService storage;
    private final MemberGuard guard;
    private final Clock clock;

    public MemberPhotoService(final MemberPhotoRepository photos, final StorageService storage, final MemberGuard guard,
                              final Clock clock) {
        this.photos = photos;
        this.storage = storage;
        this.guard = guard;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<PhotoView> photos(final CurrentUser user, final UUID memberId) {
        guard.requireManage(user, memberId);
        return photos.findByUserIdOrderByTakenAtDesc(memberId).stream()
                .map(p -> new PhotoView(p.getId(), p.getPhotoUrl(), p.getSide().label(), p.getTakenAt().toString(),
                        FrenchDates.medium(p.getTakenAt()), p.getNotes()))
                .toList();
    }

    /** Chemin {@code <membre>/<horodatage>-<vue>.<ext>}, comme l'original. */
    @Transactional
    public void upload(final CurrentUser user, final UUID memberId, final PhotoUpload upload, final InputStream content) {
        guard.requireManage(user, memberId);
        final PhotoSide side = PhotoSide.fromCode(upload.side()).orElse(PhotoSide.FRONT);
        final String path = memberId + "/" + clock.millis() + "-" + side.code() + "." + extensionOf(upload.filename());
        storage.store(new StorageKey(MemberBuckets.PHOTOS, path), content, upload.contentType());
        final LocalDate takenAt = date(upload.takenAt()).orElseGet(() -> LocalDate.now(clock));
        photos.save(new MemberPhoto(memberId, takenAt, URL_PREFIX + path, side, text(upload.notes()).orElse(null)));
    }

    @Transactional
    public void delete(final CurrentUser user, final UUID photoId) {
        final MemberPhoto photo = photos.findById(photoId).orElseThrow(() -> new AccessDeniedException("Photo introuvable"));
        guard.requireManage(user, photo.getUserId());
        photos.delete(photo);
        // L'original laissait le fichier dans le bucket ; on le retire avec la photo.
        if (photo.getPhotoUrl().startsWith(URL_PREFIX)) {
            storage.delete(new StorageKey(MemberBuckets.PHOTOS, photo.getPhotoUrl().substring(URL_PREFIX.length())));
        }
    }

    public record PhotoUpload(String filename, String contentType, String side, String takenAt, String notes) {
    }

    private static String extensionOf(final String filename) {
        final String name = Optional.ofNullable(filename).orElse("");
        final int dot = name.lastIndexOf('.');
        final String ext = dot >= 0 ? name.substring(dot + 1).toLowerCase(Locale.ROOT) : "";
        return ext.matches("[a-z0-9]{1,8}") ? ext : "jpg";
    }
}
