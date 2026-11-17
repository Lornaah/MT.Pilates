package fr.mtpilates.video;

import static fr.mtpilates.shared.web.FormValues.integer;
import static fr.mtpilates.shared.web.FormValues.text;

import fr.mtpilates.auth.CurrentUser;
import fr.mtpilates.auth.DataAccessRoles;
import fr.mtpilates.config.ConfigService;
import fr.mtpilates.config.Labels;
import fr.mtpilates.shared.ui.FrenchDates;
import java.time.Clock;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Bibliothèque vidéo : séances publiées pour tous, gestion par le staff, visionnages par membre. */
@Service
public class VideoService {

    public static final String INVALID = "Titre et URL/ID YouTube valide requis";
    private static final Limit HISTORY = Limit.of(50);

    private final VideoRepository videos;
    private final VideoViewRepository views;
    private final ConfigService config;
    private final Clock clock;

    public VideoService(final VideoRepository videos, final VideoViewRepository views, final ConfigService config,
                        final Clock clock) {
        this.videos = videos;
        this.views = views;
        this.config = config;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<VideoCard> published() {
        return cards(videos.findByPublishedTrueOrderByCreatedAtDesc());
    }

    @Transactional(readOnly = true)
    public List<VideoCard> all(final CurrentUser user) {
        requireStaff(user);
        return cards(videos.findAllByOrderByCreatedAtDesc());
    }

    @Transactional
    public void create(final CurrentUser user, final VideoForm form) {
        requireStaff(user);
        final Optional<String> youtubeId = YouTubeIds.extract(form.youtube());
        final String title = Optional.ofNullable(form.title()).map(String::trim).orElse("");
        if (title.isEmpty() || youtubeId.isEmpty()) {
            throw new IllegalArgumentException(INVALID);
        }
        final List<String> focus = Arrays.stream(Optional.ofNullable(form.focus()).orElse("").split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).toList();
        videos.save(new Video(title, text(form.description()).map(String::trim).orElse(null), youtubeId.get(),
                integer(form.duration()).orElse(null), VideoLevel.fromCode(form.level()).orElse(VideoLevel.BEGINNER),
                focus, user.id()));
    }

    @Transactional
    public void delete(final CurrentUser user, final UUID videoId) {
        requireStaff(user);
        videos.deleteById(videoId);
    }

    /** Visionnage enregistré à l'ouverture du lecteur ; une séance non publiée n'est visible que du staff. */
    @Transactional
    public void recordView(final CurrentUser user, final UUID videoId) {
        videos.findById(videoId).ifPresent(video -> views.save(new VideoView(user.id(), video, clock.instant())));
    }

    @Transactional(readOnly = true)
    public List<VideoHistoryItem> history(final CurrentUser user, final UUID memberId) {
        if (!user.id().equals(memberId) && !DataAccessRoles.isStaff(user.realRoles())) {
            throw new AccessDeniedException("Historique non consultable");
        }
        return views.findByUserIdOrderByWatchedAtDesc(memberId, HISTORY).stream()
                .map(v -> new VideoHistoryItem(v.getVideo().getTitle(), v.isCompleted(), v.getRating(),
                        FrenchDates.medium(v.getWatchedAt().atZone(clock.getZone()).toLocalDate())))
                .toList();
    }

    private List<VideoCard> cards(final List<Video> source) {
        final Labels labels = config.labels();
        return source.stream()
                .map(v -> new VideoCard(v.getId(), v.getTitle(), v.getDescription().orElse(""), v.getYoutubeId(),
                        v.getThumbnailUrl().orElse("https://i.ytimg.com/vi/" + v.getYoutubeId() + "/hqdefault.jpg"),
                        v.getLevel(), labels.videoLevel(v.getLevel()), v.getDurationMinutes(), v.getFocusAreas()))
                .toList();
    }

    private static void requireStaff(final CurrentUser user) {
        if (!DataAccessRoles.isStaff(user.realRoles())) {
            throw new AccessDeniedException("Réservé au staff");
        }
    }
}
