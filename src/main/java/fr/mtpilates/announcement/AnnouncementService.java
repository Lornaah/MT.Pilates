package fr.mtpilates.announcement;

import fr.mtpilates.announcement.AnnouncementViews.Bell;
import fr.mtpilates.announcement.AnnouncementViews.FormValues;
import fr.mtpilates.announcement.AnnouncementViews.MemberItem;
import fr.mtpilates.announcement.AnnouncementViews.StaffGroups;
import fr.mtpilates.announcement.AnnouncementViews.StaffItem;
import fr.mtpilates.auth.CurrentUser;
import fr.mtpilates.auth.DataAccessRoles;
import fr.mtpilates.shared.storage.StorageKey;
import fr.mtpilates.shared.storage.StorageService;
import fr.mtpilates.shared.ui.Markdown;
import fr.mtpilates.shared.ui.RelativeTime;
import fr.mtpilates.user.Profile;
import fr.mtpilates.user.ProfileRepository;
import java.io.InputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Annonces : gestion par le staff, lecture ciblée par les membres, cloche de notifications. */
@Service
public class AnnouncementService {

    static final String BUCKET = "announcement-images";
    private static final Instant DRAFT_THRESHOLD = Instant.parse("2099-01-01T00:00:00Z");
    private static final DateTimeFormatter PUBLISHED = DateTimeFormatter.ofPattern("d MMM yyyy 'à' HH:mm", Locale.FRENCH);
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH);
    private static final Set<String> CHANNELS = Set.of("in_app", "email", "push", "all");
    private static final Set<String> TARGETS = Set.of("all", "active", "specific", "by_coach");
    private static final Set<String> FREQUENCIES = Set.of("DAILY", "WEEKLY", "MONTHLY");
    private static final int BELL_SIZE = 5;
    private static final int EXCERPT_LENGTH = 140;

    private final AnnouncementRepository announcements;
    private final AnnouncementReadRepository reads;
    private final ProfileRepository profiles;
    private final StorageService storage;
    private final Markdown markdown;
    private final Clock clock;

    public AnnouncementService(final AnnouncementRepository announcements, final AnnouncementReadRepository reads,
                               final ProfileRepository profiles, final StorageService storage, final Markdown markdown,
                               final Clock clock) {
        this.announcements = announcements;
        this.reads = reads;
        this.profiles = profiles;
        this.storage = storage;
        this.markdown = markdown;
        this.clock = clock;
    }

    /** Brouillons (2099 et après), programmées (futur), publiées (passé), les plus récentes d'abord. */
    @Transactional(readOnly = true)
    public StaffGroups staff(final CurrentUser user) {
        requireStaff(user);
        final Instant now = clock.instant();
        final List<Announcement> all = announcements.findAllByOrderByPublishAtDesc();
        return new StaffGroups(items(all, a -> !a.getPublishAt().isBefore(DRAFT_THRESHOLD)),
                items(all, a -> a.getPublishAt().isAfter(now) && a.getPublishAt().isBefore(DRAFT_THRESHOLD)),
                items(all, a -> !a.getPublishAt().isAfter(now)));
    }

    /** v_announcements_for_user : publiées et ciblées (tous, actifs, liste, membres d'un coach), avec état de lecture. */
    @Transactional(readOnly = true)
    public List<MemberItem> forUser(final CurrentUser user) {
        final Instant now = clock.instant();
        final Optional<Profile> me = profiles.findById(user.id());
        final Set<UUID> read = reads.findByUserId(user.id()).stream().map(AnnouncementRead::getAnnouncementId)
                .collect(Collectors.toSet());
        return announcements.findByPublishAtLessThanEqualOrderByPublishAtDesc(now).stream()
                .filter(a -> targets(a.content(), user.id(), me))
                .map(a -> memberItem(a, !read.contains(a.getId()), now)).toList();
    }

    @Transactional(readOnly = true)
    public Bell bell(final CurrentUser user) {
        final List<MemberItem> all = forUser(user);
        return new Bell((int) all.stream().filter(MemberItem::unread).count(), all.stream().limit(BELL_SIZE).toList());
    }

    @Transactional
    public void markRead(final CurrentUser user, final UUID id) {
        if (!reads.existsByAnnouncementIdAndUserId(id, user.id()) && announcements.existsById(id)) {
            reads.save(new AnnouncementRead(id, user.id(), clock.instant()));
        }
    }

    @Transactional(readOnly = true)
    public FormValues formValues(final CurrentUser user, final Optional<UUID> id) {
        requireStaff(user);
        return id.flatMap(announcements::findById).map(a -> {
            final AnnouncementContent c = a.content();
            final Map<String, String> rule = parseRule(c.recurrenceRule());
            return new FormValues(Optional.of(a.getId()), c.title(), c.body(), Optional.ofNullable(c.coverImageUrl()).orElse(""),
                    c.channel(), c.target(), c.targetUserIds(), Optional.ofNullable(c.targetCoachId()).map(UUID::toString).orElse(""),
                    c.recurring(), rule.getOrDefault("FREQ", "WEEKLY"),
                    Arrays.asList(rule.getOrDefault("BYDAY", "MO").split(",")), localInput(c.publishAt()));
        }).orElseGet(() -> new FormValues(Optional.empty(), "", "", "", "in_app", "all", List.of(), "", false, "WEEKLY",
                List.of("MO"), localInput(clock.instant())));
    }

    /** Titre et contenu requis, sinon rien n'est enregistré (l'original ignorait la soumission). */
    @Transactional
    public boolean save(final CurrentUser user, final Optional<UUID> id, final AnnouncementForm form) {
        requireStaff(user);
        final String title = Optional.ofNullable(form.title()).map(String::trim).orElse("");
        final String body = Optional.ofNullable(form.body()).orElse("");
        if (title.isEmpty() || body.isBlank()) {
            return false;
        }
        final String target = form.target() != null && TARGETS.contains(form.target()) ? form.target() : "all";
        final boolean recurring = form.recurring() != null && form.recurring().contains("true");
        final AnnouncementContent content = new AnnouncementContent(title, body,
                Optional.ofNullable(form.coverImageUrl()).filter(s -> !s.isBlank()).orElse(null),
                form.channel() != null && CHANNELS.contains(form.channel()) ? form.channel() : "in_app", target,
                "specific".equals(target) ? uuids(form.targetUserIds()) : List.of(),
                "by_coach".equals(target) ? uuid(form.targetCoachId()).orElse(null) : null, recurring,
                recurrenceRule(recurring, form.recurrenceFreq(), form.recurrenceDays()),
                parseLocal(form.publishAt()).orElseGet(clock::instant));
        final Announcement announcement = id.flatMap(announcements::findById).orElseGet(() -> new Announcement(user.id()));
        announcement.update(content, user.id());
        announcements.save(announcement);
        return true;
    }

    @Transactional
    public void delete(final CurrentUser user, final UUID id) {
        requireStaff(user);
        announcements.deleteById(id);
    }

    /** Image de couverture dans le bucket public, sous un nom aléatoire. */
    public String uploadCover(final CurrentUser user, final String filename, final String contentType, final InputStream content) {
        requireStaff(user);
        final String name = Optional.ofNullable(filename).orElse("");
        final int dot = name.lastIndexOf('.');
        final String ext = dot >= 0 && name.substring(dot + 1).matches("[A-Za-z0-9]{1,8}") ? name.substring(dot + 1) : "jpg";
        final String path = UUID.randomUUID() + "." + ext;
        storage.store(new StorageKey(BUCKET, path), content, contentType);
        return "/files/" + BUCKET + "/" + path;
    }

    static boolean targets(final AnnouncementContent a, final UUID userId, final Optional<Profile> me) {
        return switch (a.target()) {
            case "all" -> true;
            case "active" -> me.flatMap(Profile::getStatus).filter("active"::equals).isPresent();
            case "specific" -> a.targetUserIds().contains(userId);
            case "by_coach" -> a.targetCoachId() != null && me.flatMap(Profile::getCoachId).filter(a.targetCoachId()::equals).isPresent();
            default -> false;
        };
    }

    /** buildRecurrenceRule : « FREQ=WEEKLY;BYDAY=MO,TH », stockée mais jamais appliquée (comme l'original). */
    static String recurrenceRule(final boolean recurring, final String freq, final List<String> days) {
        if (!recurring) {
            return null;
        }
        final String frequency = freq != null && FREQUENCIES.contains(freq) ? freq : "WEEKLY";
        final List<String> byDay = Optional.ofNullable(days).orElse(List.of()).stream().filter(d -> d.matches("[A-Z]{2}")).toList();
        return "WEEKLY".equals(frequency) && !byDay.isEmpty()
                ? "FREQ=" + frequency + ";BYDAY=" + String.join(",", byDay) : "FREQ=" + frequency;
    }

    private static Map<String, String> parseRule(final String rule) {
        if (rule == null) {
            return Map.of();
        }
        return Arrays.stream(rule.split(";")).map(p -> p.split("=", 2)).filter(kv -> kv.length == 2)
                .collect(Collectors.toMap(kv -> kv[0], kv -> kv[1], (a, b) -> b));
    }

    private List<StaffItem> items(final List<Announcement> all, final Predicate<Announcement> filter) {
        return all.stream().filter(filter).map(a -> {
            final AnnouncementContent c = a.content();
            return new StaffItem(a.getId(), c.title(), c.channel(), c.target(), c.recurring(),
                    LocalDateTime.ofInstant(c.publishAt(), zone()).format(PUBLISHED), markdown.toHtml(c.body()), a.getCoverImageUrl());
        }).toList();
    }

    private MemberItem memberItem(final Announcement a, final boolean unread, final Instant now) {
        final AnnouncementContent c = a.content();
        final String excerpt = c.body().length() > EXCERPT_LENGTH ? c.body().substring(0, EXCERPT_LENGTH) : c.body();
        return new MemberItem(a.getId(), c.title(), LocalDateTime.ofInstant(c.publishAt(), zone()).format(DAY),
                markdown.toHtml(c.body()), markdown.toHtml(excerpt), RelativeTime.between(c.publishAt(), now),
                a.getCoverImageUrl(), unread);
    }

    private String localInput(final Instant instant) {
        return LocalDateTime.ofInstant(instant, zone()).truncatedTo(ChronoUnit.MINUTES).toString();
    }

    private Optional<Instant> parseLocal(final String value) {
        try {
            return value == null || value.isBlank() ? Optional.empty()
                    : Optional.of(LocalDateTime.parse(value).atZone(zone()).toInstant());
        } catch (final DateTimeParseException e) {
            return Optional.empty();
        }
    }

    private static List<UUID> uuids(final List<String> values) {
        return Optional.ofNullable(values).orElse(List.of()).stream().map(AnnouncementService::uuid).flatMap(Optional::stream).toList();
    }

    private static Optional<UUID> uuid(final String value) {
        try {
            return value == null || value.isBlank() ? Optional.empty() : Optional.of(UUID.fromString(value));
        } catch (final IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private ZoneId zone() {
        return clock.getZone();
    }

    private static void requireStaff(final CurrentUser user) {
        if (!DataAccessRoles.isStaff(user.realRoles())) {
            throw new AccessDeniedException("Réservé au staff");
        }
    }
}
