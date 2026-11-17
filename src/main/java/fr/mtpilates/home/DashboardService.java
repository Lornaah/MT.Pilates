package fr.mtpilates.home;

import fr.mtpilates.auth.CurrentUser;
import fr.mtpilates.calendar.CalendarViews.EventView;
import fr.mtpilates.calendar.Event;
import fr.mtpilates.calendar.EventRepository;
import fr.mtpilates.home.DashboardViews.Birthday;
import fr.mtpilates.home.DashboardViews.Coach;
import fr.mtpilates.home.DashboardViews.Member;
import fr.mtpilates.home.DashboardViews.Staff;
import fr.mtpilates.home.DashboardViews.Stats;
import fr.mtpilates.home.DashboardViews.Upcoming;
import fr.mtpilates.home.DashboardViews.WeekDay;
import fr.mtpilates.home.DashboardViews.WeekEvent;
import fr.mtpilates.calendar.CalendarService;
import fr.mtpilates.nutrition.NutritionPlanQueries;
import fr.mtpilates.user.Profile;
import fr.mtpilates.user.ProfileRepository;
import fr.mtpilates.video.VideoViewRepository;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.MonthDay;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Tableaux de bord de /app : staff (stats, agenda, RDV, anniversaires) et membre (série, semaine, liens). */
@Service
public class DashboardService {

    private static final Locale FR = Locale.FRENCH;
    private static final DateTimeFormatter DAY_LONG = DateTimeFormatter.ofPattern("EEEE d MMMM", FR);
    private static final DateTimeFormatter UPCOMING = DateTimeFormatter.ofPattern("EEE d MMM • HH:mm", FR);
    private static final DateTimeFormatter MONTH_SHORT = DateTimeFormatter.ofPattern("MMM", FR);
    private static final DateTimeFormatter WEEKDAY = DateTimeFormatter.ofPattern("EEE", FR);
    private static final DateTimeFormatter WEEK_OF = DateTimeFormatter.ofPattern("d MMM", FR);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final int UPCOMING_LIMIT = 5;
    private static final int BIRTHDAY_HORIZON_DAYS = 7;
    private static final int RECENT_VIEWS = 60;

    private final fr.mtpilates.member.MemberService members;
    private final ProfileRepository profiles;
    private final EventRepository events;
    private final CalendarService calendar;
    private final VideoViewRepository views;
    private final NutritionPlanQueries nutrition;
    private final fr.mtpilates.member.MemberAccessPolicy policy;
    private final Clock clock;

    public DashboardService(final fr.mtpilates.member.MemberService members, final ProfileRepository profiles,
                            final EventRepository events, final CalendarService calendar, final VideoViewRepository views,
                            final NutritionPlanQueries nutrition,
                            final fr.mtpilates.member.MemberAccessPolicy policy, final Clock clock) {
        this.members = members;
        this.profiles = profiles;
        this.events = events;
        this.calendar = calendar;
        this.views = views;
        this.nutrition = nutrition;
        this.policy = policy;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Staff staff(final CurrentUser user) {
        final LocalDate today = LocalDate.now(clock);
        final List<Profile> visible = visibleProfiles(user);
        final Instant now = clock.instant();
        return new Staff(profiles.findById(user.id()).flatMap(Profile::getFirstName).orElse(""), stats(visible),
                today.format(DAY_LONG), todayEvents(user, today),
                upcoming(user, "one_on_one", now), upcoming(user, "admin_task", now), birthdays(visible, today));
    }

    @Transactional(readOnly = true)
    public Member member(final CurrentUser user) {
        final Optional<Profile> profile = profiles.findById(user.id());
        // Fidèle aux règles d'accès de l'original : un membre ne lit pas la fiche de son coach, d'où « À assigner ».
        final Optional<Coach> coach = profile.flatMap(Profile::getCoachId).flatMap(profiles::findById)
                .filter(c -> policy.canView(user, c.getId(), c.getCoachId())).map(c ->
                new Coach(c.getFirstName().orElse(""), (c.getFirstName().orElse("") + " " + c.getLastName().orElse("")).trim(),
                        c.getAvatarUrl().filter(s -> !s.isEmpty()), c.getFirstName().filter(s -> !s.isEmpty())
                        .map(s -> s.substring(0, 1)).orElse("M")));
        final LocalDate today = LocalDate.now(clock);
        final LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        final Instant monthStart = today.withDayOfMonth(1).atStartOfDay(zone()).toInstant();
        final List<Event> week = events.findBetween(weekStart.atStartOfDay(zone()).toInstant(),
                weekStart.plusDays(7).atStartOfDay(zone()).toInstant()).stream()
                .filter(e -> e.getMemberId().filter(user.id()::equals).isPresent()).toList();
        return new Member(profile.flatMap(Profile::getFirstName), coach, streak(user.id(), today),
                views.countByUserIdAndWatchedAtGreaterThanEqual(user.id(), monthStart),
                profile.flatMap(Profile::getWeeklyTargetSessions).orElse(2) * 4, weekStart.format(WEEK_OF),
                IntStream.range(0, 7).mapToObj(weekStart::plusDays).map(d -> weekDay(d, today, week)).toList(),
                nutrition.hasActivePlan(user.id()));
    }

    private List<Profile> visibleProfiles(final CurrentUser user) {
        final Set<UUID> ids = members.visibleTo(user, new fr.mtpilates.member.MemberFilters("", "all", "all")).stream()
                .map(fr.mtpilates.member.MemberRow::id).collect(Collectors.toSet());
        return profiles.findAllById(ids);
    }

    private static Stats stats(final List<Profile> visible) {
        return new Stats(count(visible, "active"), count(visible, "paused"), count(visible, "archived"), visible.size());
    }

    private static long count(final List<Profile> visible, final String status) {
        return visible.stream().filter(p -> p.getStatus().filter(status::equals).isPresent()).count();
    }

    private List<EventView> todayEvents(final CurrentUser user, final LocalDate today) {
        final List<Event> list = events.findForStaffBetween(user.id(), today.atStartOfDay(zone()).toInstant(),
                today.plusDays(1).atStartOfDay(zone()).toInstant().minusMillis(1));
        return calendar.viewsOf(list);
    }

    private List<Upcoming> upcoming(final CurrentUser user, final String type, final Instant now) {
        return events.findUpcomingOfType(user.id(), type, now, Limit.of(UPCOMING_LIMIT)).stream().map(e -> {
            final LocalDateTime start = LocalDateTime.ofInstant(e.getStartsAt(), zone());
            return new Upcoming(e.getId(), e.getTitle(), start.format(MONTH_SHORT), String.valueOf(start.getDayOfMonth()),
                    start.format(UPCOMING), e.getMemberId());
        }).toList();
    }

    /** Anniversaires dans les 7 prochains jours, avec un lien WhatsApp si le téléphone est connu. */
    private static List<Birthday> birthdays(final List<Profile> visible, final LocalDate today) {
        final LocalDate horizon = today.plusDays(BIRTHDAY_HORIZON_DAYS);
        return visible.stream()
                .flatMap(p -> p.getDateOfBirth().map(dob -> new UpcomingBirthday(p, nextBirthday(dob, today))).stream())
                .filter(b -> !b.date().isAfter(horizon))
                .sorted(Comparator.comparing(UpcomingBirthday::date))
                .map(b -> new Birthday(nameOrEmail(b.profile()), b.date().format(DAY_LONG),
                        whatsapp(b.profile().getPhone(), "Joyeux anniversaire " + b.profile().getFirstName().orElse("")
                                + " 🎉 ! Toute l'équipe te souhaite une très belle journée.")))
                .toList();
    }

    private record UpcomingBirthday(Profile profile, LocalDate date) {
    }

    private static String nameOrEmail(final Profile p) {
        final String name = (p.getFirstName().orElse("") + " " + p.getLastName().orElse("")).trim();
        return name.isEmpty() ? p.getEmail() : name;
    }

    private static LocalDate nextBirthday(final LocalDate dob, final LocalDate today) {
        final LocalDate candidate = MonthDay.from(dob).atYear(today.getYear());
        return candidate.isBefore(today) ? MonthDay.from(dob).atYear(today.getYear() + 1) : candidate;
    }

    /** whatsappLink de l'original : chiffres du numéro, message encodé. */
    static Optional<String> whatsapp(final Optional<String> phone, final String message) {
        return phone.map(p -> p.replaceAll("\\D", "")).filter(d -> !d.isEmpty())
                .map(d -> "https://wa.me/" + d + "?text=" + URLEncoder.encode(message, StandardCharsets.UTF_8).replace("+", "%20"));
    }

    /** Jours consécutifs avec au moins une séance vidéo, jusqu'à aujourd'hui. */
    private long streak(final UUID userId, final LocalDate today) {
        final Set<LocalDate> days = views.findRecentWatchedAt(userId, Limit.of(RECENT_VIEWS)).stream()
                .map(i -> LocalDate.ofInstant(i, zone())).collect(Collectors.toSet());
        long streak = 0;
        LocalDate cursor = today;
        while (days.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }

    private WeekDay weekDay(final LocalDate day, final LocalDate today, final List<Event> week) {
        final List<WeekEvent> dayEvents = week.stream()
                .filter(e -> LocalDate.ofInstant(e.getStartsAt(), zone()).equals(day))
                .map(e -> new WeekEvent(LocalDateTime.ofInstant(e.getStartsAt(), zone()).format(TIME), e.getTitle()))
                .toList();
        return new WeekDay(day.format(WEEKDAY), String.valueOf(day.getDayOfMonth()), day.equals(today), dayEvents);
    }

    private ZoneId zone() {
        return clock.getZone();
    }
}
