package fr.mtpilates.training;

import fr.mtpilates.auth.CurrentUser;
import fr.mtpilates.auth.DataAccessRoles;
import fr.mtpilates.member.MemberGuard;
import fr.mtpilates.shared.ui.FrenchDates;
import fr.mtpilates.training.PlanningViews.DayView;
import fr.mtpilates.training.PlanningViews.MemberProgram;
import fr.mtpilates.training.PlanningViews.MemberProgramSection;
import fr.mtpilates.training.PlanningViews.Planning;
import fr.mtpilates.training.PlanningViews.SessionLink;
import fr.mtpilates.training.PlanningViews.WeekView;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Côté membre : planification active, jours faits ; côté fiche : programmes du membre. */
@Service
public class PlanningService {

    private static final Map<String, String> TYPE_LABELS = Map.of("workout", "Séance", "rest", "Repos", "review", "Bilan",
            "nutrition", "Nutrition", "mixed", "Mixte");
    private static final List<Map.Entry<String, List<String>>> SECTIONS = List.of(
            Map.entry("En cours", List.of("active")),
            Map.entry("À venir", List.of("scheduled", "draft")),
            Map.entry("Terminés", List.of("finished")));

    private final ProgramRepository programs;
    private final ProgramDayCompletionRepository completions;
    private final MemberGuard guard;
    private final Clock clock;

    public PlanningService(final ProgramRepository programs, final ProgramDayCompletionRepository completions,
                           final MemberGuard guard, final Clock clock) {
        this.programs = programs;
        this.completions = completions;
        this.guard = guard;
        this.clock = clock;
    }

    /** getActiveForMember : programme actif du membre ou commun, le plus récent par date de début. */
    @Transactional(readOnly = true)
    public Optional<Planning> active(final CurrentUser user) {
        final Optional<Program> program = programs.findForMember(user.id()).stream()
                .filter(p -> "active".equals(p.getStatus()))
                .min(Comparator.comparing((Program p) -> p.getStartDate().orElse(LocalDate.MIN), Comparator.reverseOrder()));
        final Set<UUID> done = completions.findByUserId(user.id()).stream().map(ProgramDayCompletion::getProgramDayId)
                .collect(Collectors.toSet());
        return program.map(p -> planning(p, done));
    }

    @Transactional
    public void toggleDone(final CurrentUser user, final UUID dayId) {
        completions.findByProgramDayIdAndUserId(dayId, user.id()).ifPresentOrElse(completions::delete,
                () -> completions.save(new ProgramDayCompletion(dayId, user.id(), clock.instant())));
    }

    /** Programmes du membre et programmes communs, rangés En cours / À venir / Terminés (modèles non listés). */
    @Transactional(readOnly = true)
    public List<MemberProgramSection> memberPrograms(final CurrentUser user, final UUID memberId) {
        if (!DataAccessRoles.isStaff(user.realRoles()) && !guard.canManage(user, memberId)) {
            throw new AccessDeniedException("Programmes non consultables");
        }
        final List<Program> all = programs.findForMember(memberId);
        return SECTIONS.stream()
                .map(s -> new MemberProgramSection(s.getKey(), s.getValue().stream()
                        .flatMap(status -> all.stream().filter(p -> status.equals(p.getStatus())))
                        .map(p -> new MemberProgram(p.getId(), p.getTitle(), ProgramStatus.of(p.getStatus()).label(),
                                p.getStartDate().map(FrenchDates::medium), p.getDurationWeeks().filter(w -> w > 0)))
                        .toList()))
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean hasPrograms(final UUID memberId) {
        return !programs.findForMember(memberId).isEmpty();
    }

    private Planning planning(final Program p, final Set<UUID> done) {
        final List<WeekView> weeks = p.getWeeks().stream().map(w -> new WeekView(w.getWeekNumber(), w.getComment(),
                IntStream.rangeClosed(1, 7).mapToObj(n -> w.getDays().stream().filter(d -> d.getDayOfWeek() == n).findFirst()
                        .map(d -> day(d, done))).toList())).toList();
        final LocalDate today = LocalDate.now(clock);
        final int current = currentWeek(p, weeks.size(), today);
        final int todayDow = today.getDayOfWeek().getValue();
        final Optional<DayView> todayDay = weeks.isEmpty() ? Optional.empty() : weeks.get(current).days().get(todayDow - 1);
        return new Planning(p.getTitle(), p.getDescription(), weeks, current, todayDay,
                PlanningViews.DAY_LABELS.get(todayDow - 1));
    }

    /** computeCurrentWeekIndex : semaines écoulées depuis la date de début, bornées au programme. */
    private static int currentWeek(final Program p, final int weekCount, final LocalDate today) {
        return p.getStartDate().map(start -> {
            final long index = Math.floorDiv(ChronoUnit.DAYS.between(start, today), 7);
            return (int) Math.max(0, Math.min(index, weekCount - 1));
        }).orElse(0);
    }

    private static DayView day(final ProgramDay d, final Set<UUID> done) {
        final String type = d.getDayType();
        final boolean workout = "workout".equals(type) || "mixed".equals(type);
        final List<SessionLink> sessions = d.getSessions().stream().map(s -> new SessionLink(s.getId(), s.getTitle())).toList();
        final String title = switch (type) {
            case "workout", "mixed" -> sessions.isEmpty() ? "Séance"
                    : sessions.stream().map(SessionLink::title).collect(Collectors.joining(" + "));
            case "nutrition" -> "Plan nutrition";
            case "review" -> "Bilan hebdo";
            default -> "Repos";
        };
        final String icon = switch (type) {
            case "rest" -> "bed";
            case "review" -> "clipboard-check";
            case "nutrition" -> "utensils-crossed";
            default -> "dumbbell";
        };
        return new DayView(d.getId(), PlanningViews.DAY_LABELS.get(d.getDayOfWeek() - 1),
                TYPE_LABELS.getOrDefault(type, type), icon, title, d.getComment(), workout, sessions, done.contains(d.getId()));
    }
}
