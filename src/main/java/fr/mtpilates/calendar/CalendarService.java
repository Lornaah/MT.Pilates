package fr.mtpilates.calendar;

import fr.mtpilates.auth.CurrentUser;
import fr.mtpilates.auth.DataAccessRoles;
import fr.mtpilates.calendar.CalendarViews.CoachChip;
import fr.mtpilates.calendar.CalendarViews.DayGroup;
import fr.mtpilates.calendar.CalendarViews.EventView;
import fr.mtpilates.calendar.CalendarViews.MemberOption;
import fr.mtpilates.calendar.CalendarViews.Month;
import fr.mtpilates.calendar.CalendarViews.MonthCell;
import fr.mtpilates.member.CoachBadges;
import fr.mtpilates.member.MemberFilters;
import fr.mtpilates.member.MemberService;
import fr.mtpilates.user.AppRole;
import fr.mtpilates.user.Profile;
import fr.mtpilates.user.ProfileRepository;
import fr.mtpilates.user.UserRole;
import fr.mtpilates.user.UserRoleRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.time.DayOfWeek;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Agenda du staff : ses événements, calendrier général de l'owner, création et suppression. */
@Service
public class CalendarService {

    private static final DateTimeFormatter DAY_HEADER = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH);
    private static final DateTimeFormatter MONTH_TITLE = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH);
    private static final int CELL_EVENTS = 3;

    private final EventRepository events;
    private final ProfileRepository profiles;
    private final UserRoleRepository roles;
    private final MemberService members;
    private final Clock clock;

    public CalendarService(final EventRepository events, final ProfileRepository profiles, final UserRoleRepository roles,
                           final MemberService members, final Clock clock) {
        this.events = events;
        this.profiles = profiles;
        this.roles = roles;
        this.members = members;
        this.clock = clock;
    }

    /** « Mon agenda » : événements animés ou créés par l'utilisateur. */
    @Transactional(readOnly = true)
    public List<EventView> mine(final CurrentUser user) {
        requireStaff(user);
        return views(events.findForStaff(user.id()));
    }

    /** « Calendrier général » de l'owner, filtrable par coach. */
    @Transactional(readOnly = true)
    public List<EventView> general(final CurrentUser user, final Optional<UUID> coach) {
        if (!user.isRealOwner()) {
            throw new AccessDeniedException("Réservé à l'owner");
        }
        return views(events.findAllByOrderByStartsAtAsc()).stream()
                .filter(e -> coach.isEmpty() || e.coachId().equals(coach)).toList();
    }

    /** Équipe affichée en filtre : owners, coachs et setters (listStaffSummaries). */
    @Transactional(readOnly = true)
    public List<CoachChip> staff() {
        final List<UUID> ids = List.of(AppRole.OWNER, AppRole.COACH, AppRole.SETTER).stream()
                .flatMap(r -> roles.findByRole(r).stream()).map(UserRole::getUserId).distinct().toList();
        return profiles.findByIdInOrderByCreatedAtAsc(ids).stream()
                .map(p -> new CoachChip(p.getId(), nameOrEmail(p), CoachBadges.classFor(Optional.of(p.getId())))).toList();
    }

    @Transactional(readOnly = true)
    public List<MemberOption> memberOptions(final CurrentUser user) {
        return members.visibleTo(user, new MemberFilters("", MemberFilters.ALL, MemberFilters.ALL)).stream()
                .map(m -> new MemberOption(m.id(), m.name() + " — " + m.email())).toList();
    }

    public List<DayGroup> byDay(final List<EventView> list) {
        final Map<String, List<EventView>> grouped = list.stream().collect(Collectors.groupingBy(EventView::startLabel,
                LinkedHashMap::new, Collectors.toList()));
        return grouped.entrySet().stream().map(e -> new DayGroup(e.getKey(), e.getValue())).toList();
    }

    /** Grille du mois, semaines du lundi au dimanche, trois événements par case au plus. */
    public Month month(final List<EventView> list, final String key) {
        final YearMonth month = parseMonth(key);
        final LocalDate today = LocalDate.now(clock);
        final LocalDate start = month.atDay(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        final LocalDate end = month.atEndOfMonth().with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        final Map<String, List<EventView>> byDay = list.stream().collect(Collectors.groupingBy(e -> e.start().substring(0, 10)));
        final List<MonthCell> cells = start.datesUntil(end.plusDays(1)).map(d -> {
            final List<EventView> dayEvents = byDay.getOrDefault(d.toString(), List.of());
            return new MonthCell(d.toString(), String.valueOf(d.getDayOfMonth()), YearMonth.from(d).equals(month),
                    d.equals(today), dayEvents.stream().limit(CELL_EVENTS).toList(), Math.max(0, dayEvents.size() - CELL_EVENTS));
        }).toList();
        return new Month(month.toString(), month.format(MONTH_TITLE), month.minusMonths(1).toString(),
                month.plusMonths(1).toString(), cells);
    }

    public List<EventView> onDay(final List<EventView> list, final String iso) {
        return list.stream().filter(e -> e.start().startsWith(iso)).toList();
    }

    /** Événement d'une heure, privé, l'auteur en coach (EventFormDialog). */
    @Transactional
    public void create(final CurrentUser user, final EventForm form) {
        requireStaff(user);
        final String title = Optional.ofNullable(form.title()).orElse("");
        final Optional<LocalDateTime> start = parseDateTime(form.startsAt());
        if (title.isEmpty() || start.isEmpty()) {
            throw new IllegalArgumentException("Titre et date/heure requis");
        }
        final var startsAt = start.get().atZone(zone()).toInstant();
        events.save(new Event(title, Optional.ofNullable(form.description()).filter(s -> !s.isEmpty()).orElse(null),
                EventType.fromCode(form.type()).orElse(EventType.ONE_ON_ONE), startsAt, startsAt.plus(Duration.ofHours(1)),
                parseUuid(form.memberId()).orElse(null), user.id()));
    }

    @Transactional
    public void delete(final CurrentUser user, final UUID id) {
        requireStaff(user);
        events.findById(id).filter(e -> user.isRealOwner() || e.getCoachId().filter(user.id()::equals).isPresent()
                        || e.getCreatedBy().filter(user.id()::equals).isPresent())
                .ifPresent(events::delete);
    }

    public String currentMonth() {
        return YearMonth.now(clock).toString();
    }

    /** Vues d'événements déjà chargés (tableau de bord). */
    @Transactional(readOnly = true)
    public List<EventView> viewsOf(final List<Event> source) {
        return views(source);
    }

    List<EventView> views(final List<Event> source) {
        final Map<UUID, String> names = staff().stream().collect(Collectors.toMap(CoachChip::id, CoachChip::name));
        return source.stream().map(e -> view(e, names)).toList();
    }

    private EventView view(final Event e, final Map<UUID, String> names) {
        final LocalDateTime start = LocalDateTime.ofInstant(e.getStartsAt(), zone());
        final LocalDateTime end = LocalDateTime.ofInstant(e.getEndsAt(), zone());
        return new EventView(e.getId(), start.toString(), end.toString(), start.format(DAY_HEADER),
                EventType.fromCode(e.getType()).orElse(EventType.ONE_ON_ONE), e.isPrivate(), e.getCoachId(),
                e.getCoachId().map(names::get), CoachBadges.classFor(e.getCoachId()), e.getTitle(), e.getDescription(),
                e.getLocation(), e.hasMeeting());
    }

    private ZoneId zone() {
        return clock.getZone();
    }

    private YearMonth parseMonth(final String key) {
        try {
            return key == null || key.isBlank() ? YearMonth.now(clock) : YearMonth.parse(key);
        } catch (final DateTimeParseException e) {
            return YearMonth.now(clock);
        }
    }

    private static Optional<LocalDateTime> parseDateTime(final String value) {
        try {
            return value == null || value.isBlank() ? Optional.empty() : Optional.of(LocalDateTime.parse(value));
        } catch (final DateTimeParseException e) {
            return Optional.empty();
        }
    }

    private static Optional<UUID> parseUuid(final String value) {
        try {
            return value == null || value.isBlank() || "none".equals(value) ? Optional.empty() : Optional.of(UUID.fromString(value));
        } catch (final IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private static String nameOrEmail(final Profile p) {
        final String name = (p.getFirstName().orElse("") + " " + p.getLastName().orElse("")).trim();
        return name.isEmpty() ? p.getEmail() : name;
    }

    private static void requireStaff(final CurrentUser user) {
        if (!DataAccessRoles.isStaff(user.realRoles())) {
            throw new AccessDeniedException("Réservé au staff");
        }
    }
}
