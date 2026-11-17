package fr.mtpilates.habit;

import fr.mtpilates.auth.CurrentUser;
import fr.mtpilates.auth.DataAccessRoles;
import fr.mtpilates.habit.HabitViews.Assignee;
import fr.mtpilates.habit.HabitViews.LibraryItem;
import fr.mtpilates.habit.HabitViews.TodayItem;
import fr.mtpilates.habit.HabitViews.TrackedDay;
import fr.mtpilates.habit.HabitViews.TrackedHabit;
import fr.mtpilates.habit.HabitViews.Tracking;
import fr.mtpilates.member.MemberFilters;
import fr.mtpilates.member.MemberGuard;
import fr.mtpilates.member.MemberService;
import fr.mtpilates.shared.ui.FrenchDates;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Habitudes : bibliothèque du staff, assignations, cases du jour du membre, suivi sur la fiche. */
@Service
public class HabitService {

    private static final Map<String, String> FREQUENCY_LABELS = Map.of("daily", "Quotidien", "weekly", "Hebdomadaire",
            "custom", "Personnalisé");
    private static final Map<String, String> FREQUENCY_VARIANTS = Map.of("daily", "coach", "weekly", "pop", "custom", "warm");
    private static final int STREAK_WINDOW_DAYS = 60;
    private static final List<Integer> RANGES = List.of(7, 30, 90);
    private static final DateTimeFormatter DAY_TITLE = DateTimeFormatter.ofPattern("EEE d MMM", Locale.FRENCH);

    private final HabitRepository habits;
    private final HabitAssignmentRepository assignments;
    private final HabitCompletionRepository completions;
    private final MemberService members;
    private final MemberGuard guard;
    private final Clock clock;

    public HabitService(final HabitRepository habits, final HabitAssignmentRepository assignments,
                        final HabitCompletionRepository completions, final MemberService members, final MemberGuard guard,
                        final Clock clock) {
        this.habits = habits;
        this.assignments = assignments;
        this.completions = completions;
        this.members = members;
        this.guard = guard;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<LibraryItem> library(final CurrentUser user) {
        requireStaff(user);
        return habits.findAllByOrderByCreatedAtDesc().stream().map(HabitService::item).toList();
    }

    @Transactional(readOnly = true)
    public Optional<LibraryItem> find(final CurrentUser user, final UUID id) {
        requireStaff(user);
        return habits.findById(id).map(HabitService::item);
    }

    /** Sans titre, rien n'est enregistré (l'original ignorait la soumission). */
    @Transactional
    public boolean save(final CurrentUser user, final Optional<UUID> id, final HabitForm form) {
        requireStaff(user);
        final String title = Optional.ofNullable(form.title()).map(String::trim).orElse("");
        if (title.isEmpty()) {
            return false;
        }
        final Habit habit = id.flatMap(habits::findById).orElseGet(() -> new Habit(user.id()));
        habit.update(title, Optional.ofNullable(form.description()).map(String::trim).filter(s -> !s.isEmpty()).orElse(null),
                HabitIcons.isKnown(form.icon()) ? form.icon() : "sparkles",
                form.frequency() != null && FREQUENCY_LABELS.containsKey(form.frequency()) ? form.frequency() : "daily", checked(form.global()));
        habits.save(habit);
        return true;
    }

    @Transactional
    public void delete(final CurrentUser user, final UUID id) {
        requireStaff(user);
        habits.deleteById(id);
    }

    /** Membres actifs visibles, cochés s'ils ont déjà l'habitude. */
    @Transactional(readOnly = true)
    public List<Assignee> assignees(final CurrentUser user, final UUID habitId) {
        requireStaff(user);
        final Set<UUID> assigned = assignments.findByHabitId(habitId).stream().map(HabitAssignment::getUserId)
                .collect(Collectors.toSet());
        return members.visibleTo(user, new MemberFilters("", MemberFilters.ALL, "active")).stream()
                .map(m -> new Assignee(m.id(), m.name(), m.email(), assigned.contains(m.id()))).toList();
    }

    /** Remplace l'ensemble des assignés par différence ; chaque membre ajouté ou retiré doit être suivi (staff ∧ manage). */
    @Transactional
    public void setAssignments(final CurrentUser user, final UUID habitId, final List<UUID> userIds) {
        requireStaff(user);
        final Set<UUID> existing = assignments.findByHabitId(habitId).stream().map(HabitAssignment::getUserId)
                .collect(Collectors.toSet());
        final Set<UUID> target = new HashSet<>(userIds);
        target.stream().filter(id -> !existing.contains(id)).forEach(id -> {
            guard.requireManage(user, id);
            assignments.save(new HabitAssignment(habitId, id));
        });
        existing.stream().filter(id -> !target.contains(id)).forEach(id -> {
            guard.requireManage(user, id);
            assignments.deleteByHabitIdAndUserId(habitId, id);
        });
    }

    /** « Mes habitudes du jour » : case cochée aujourd'hui et série en cours. */
    @Transactional(readOnly = true)
    public List<TodayItem> today(final CurrentUser user) {
        final LocalDate today = LocalDate.now(clock);
        final Map<UUID, Set<LocalDate>> done = completionsByHabit(user.id(), today.minusDays(STREAK_WINDOW_DAYS), today);
        return forMember(user.id()).stream().map(h -> {
            final Set<LocalDate> dates = done.getOrDefault(h.getId(), Set.of());
            return new TodayItem(h.getId(), h.getTitle(), h.getDescription(), HabitIcons.lucide(h.getIcon()),
                    dates.contains(today), streak(dates, today));
        }).toList();
    }

    @Transactional
    public void toggleToday(final CurrentUser user, final UUID habitId) {
        final LocalDate today = LocalDate.now(clock);
        completions.findByHabitIdAndUserIdAndCompletedDate(habitId, user.id(), today).ifPresentOrElse(completions::delete,
                () -> completions.save(new HabitCompletion(habitId, user.id(), today)));
    }

    /** Suivi des habitudes d'un membre sur 7, 30 ou 90 jours (taux par habitude et global). */
    @Transactional(readOnly = true)
    public Tracking tracking(final CurrentUser user, final UUID memberId, final int requestedRange) {
        guard.requireManage(user, memberId);
        final int range = RANGES.contains(requestedRange) ? requestedRange : 30;
        final LocalDate today = LocalDate.now(clock);
        final LocalDate from = today.minusDays(range - 1L);
        final Map<UUID, Set<LocalDate>> done = completionsByHabit(memberId, from, today);
        final List<LocalDate> days = IntStream.range(0, range).mapToObj(from::plusDays).toList();
        final List<Habit> memberHabits = forMember(memberId);
        final List<TrackedHabit> tracked = memberHabits.stream().map(h -> {
            final Set<LocalDate> dates = done.getOrDefault(h.getId(), Set.of());
            return new TrackedHabit(h.getTitle(), HabitIcons.lucide(h.getIcon()), streak(dates, today),
                    days.stream().map(d -> new TrackedDay(d.format(DAY_TITLE) + (dates.contains(d) ? " ✓" : ""), dates.contains(d))).toList(),
                    dates.size(), Math.round(dates.size() * 100f / range));
        }).toList();
        final int total = memberHabits.size() * range;
        final int globalRate = total == 0 ? 0 : Math.round(tracked.stream().mapToInt(TrackedHabit::done).sum() * 100f / total);
        return new Tracking(range, memberHabits.size(), globalRate, FrenchDates.shortDate(from), tracked);
    }

    private List<Habit> forMember(final UUID memberId) {
        final List<UUID> assigned = assignments.findByUserId(memberId).stream().map(HabitAssignment::getHabitId).toList();
        return habits.findGlobalOrIn(assigned.isEmpty() ? List.of(new UUID(0, 0)) : assigned);
    }

    private Map<UUID, Set<LocalDate>> completionsByHabit(final UUID userId, final LocalDate from, final LocalDate to) {
        return completions.findByUserIdAndCompletedDateBetween(userId, from, to).stream()
                .collect(Collectors.groupingBy(HabitCompletion::getHabitId,
                        Collectors.mapping(HabitCompletion::getCompletedDate, Collectors.toSet())));
    }

    /** Jours consécutifs cochés jusqu'à aujourd'hui inclus. */
    static int streak(final Set<LocalDate> dates, final LocalDate today) {
        int streak = 0;
        LocalDate cursor = today;
        while (dates.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }

    private static LibraryItem item(final Habit h) {
        final String frequency = Optional.ofNullable(h.getFrequency()).orElse("daily");
        return new LibraryItem(h.getId(), h.getTitle(), h.getDescription(), HabitIcons.lucide(h.getIcon()),
                h.getIcon().orElse("sparkles"), frequency, FREQUENCY_LABELS.getOrDefault(frequency, "Quotidien"),
                FREQUENCY_VARIANTS.getOrDefault(frequency, "coach"), h.isGlobal());
    }

    /** Case + champ caché : « false,true » quand la case est cochée, « false » sinon. */
    private static boolean checked(final String value) {
        return value != null && value.contains("true");
    }

    private static void requireStaff(final CurrentUser user) {
        if (!DataAccessRoles.isStaff(user.realRoles())) {
            throw new AccessDeniedException("Réservé au staff");
        }
    }
}
