package fr.mtpilates.training;

import static fr.mtpilates.shared.web.FormValues.list;
import static fr.mtpilates.shared.web.FormValues.text;

import fr.mtpilates.auth.CurrentUser;
import fr.mtpilates.auth.DataAccessRoles;
import fr.mtpilates.config.ConfigService;
import fr.mtpilates.video.YouTubeIds;
import java.text.Collator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Bibliothèque d'exercices du staff. */
@Service
public class ExerciseService {

    private final ExerciseRepository exercises;
    private final ConfigService config;

    public ExerciseService(final ExerciseRepository exercises, final ConfigService config) {
        this.exercises = exercises;
        this.config = config;
    }

    @Transactional(readOnly = true)
    public ExerciseLibraryView library(final CurrentUser user) {
        requireStaff(user);
        final List<Exercise> all = exercises.findAllByOrderByCreatedAtDesc();
        final List<ExerciseCard> cards = all.stream().map(e -> card(e, user)).toList();
        return new ExerciseLibraryView(cards, cards.stream().filter(ExerciseCard::mine).count(),
                cards.stream().filter(ExerciseCard::favorite).count(),
                sortedUnion(ExerciseCatalog.MUSCLE_GROUPS, all.stream().flatMap(e -> e.content().muscleGroups().stream())),
                sortedUnion(ExerciseCatalog.EQUIPMENT, all.stream().flatMap(e -> e.content().equipment().stream())),
                sortedUnion(ExerciseCatalog.TYPES, all.stream().map(e -> e.content().exerciseType()).filter(Objects::nonNull)),
                sortedUnion(config.labels().objectives(), all.stream().flatMap(e -> e.content().objectives().stream())));
    }

    @Transactional(readOnly = true)
    public Optional<ExerciseContent> content(final CurrentUser user, final UUID id) {
        requireStaff(user);
        return exercises.findById(id).map(Exercise::content);
    }

    /** Sans titre, rien n'est enregistré (l'original ignorait le clic). */
    @Transactional
    public boolean save(final CurrentUser user, final Optional<UUID> id, final ExerciseForm form) {
        requireStaff(user);
        final String title = text(form.title()).map(String::trim).orElse("");
        if (title.isEmpty()) {
            return false;
        }
        final ExerciseContent content = new ExerciseContent(title, trimmed(form.description()), trimmed(form.videoUrl()),
                trimmed(form.demoUrl()), trimmed(form.thumbnailUrl()), list(form.muscleGroups()), list(form.equipment()),
                text(form.exerciseType()).orElse(null), list(form.objectives()),
                ExerciseAttributes.parse(form.defaultAttributes()), !"false".equals(form.published()));
        id.flatMap(exercises::findById).ifPresentOrElse(e -> e.apply(content, user.id()),
                () -> exercises.save(new Exercise(content, user.id())));
        return true;
    }

    @Transactional
    public void toggleFavorite(final CurrentUser user, final UUID id) {
        requireStaff(user);
        exercises.findById(id).ifPresent(Exercise::toggleFavorite);
    }

    @Transactional
    public void duplicate(final CurrentUser user, final UUID id) {
        requireStaff(user);
        exercises.findById(id).ifPresent(e -> exercises.save(e.duplicate(user.id())));
    }

    @Transactional
    public void delete(final CurrentUser user, final UUID id) {
        requireStaff(user);
        exercises.deleteById(id);
    }

    static void requireStaff(final CurrentUser user) {
        if (!DataAccessRoles.isStaff(user.realRoles())) {
            throw new AccessDeniedException("Réservé au staff");
        }
    }

    /** Miniature : URL fournie, sinon vignette YouTube de la vidéo ou de la démo. */
    static Optional<String> thumbnail(final ExerciseContent c) {
        return Optional.ofNullable(c.thumbnailUrl()).filter(s -> !s.isEmpty())
                .or(() -> youtubeThumbnail(c.videoUrl()))
                .or(() -> youtubeThumbnail(c.demoUrl()));
    }

    private static Optional<String> youtubeThumbnail(final String url) {
        return YouTubeIds.extract(url).map(id -> "https://i.ytimg.com/vi/" + id + "/hqdefault.jpg");
    }

    private static ExerciseCard card(final Exercise e, final CurrentUser user) {
        final ExerciseContent c = e.content();
        return new ExerciseCard(e.getId(), c.title(), thumbnail(c), c.muscleGroups(), c.equipment(), c.objectives(),
                Optional.ofNullable(c.exerciseType()).orElse(""), e.isFavorite(),
                e.getCreatedBy().filter(user.id()::equals).isPresent());
    }

    private static String trimmed(final String value) {
        return text(value).map(String::trim).filter(s -> !s.isEmpty()).orElse(null);
    }

    /** uniqSorted de l'original : valeurs uniques, triées à la française sans tenir compte de la casse ni des accents. */
    static List<String> sortedUnion(final List<String> suggestions, final Stream<String> used) {
        final Collator collator = Collator.getInstance(Locale.FRENCH);
        collator.setStrength(Collator.PRIMARY);
        return Stream.concat(suggestions.stream(), used).map(String::trim).filter(s -> !s.isEmpty()).distinct()
                .sorted(collator).toList();
    }
}
