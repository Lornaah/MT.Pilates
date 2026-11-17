package fr.mtpilates.training;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.mtpilates.auth.CurrentUser;
import fr.mtpilates.member.MemberFilters;
import fr.mtpilates.member.MemberGuard;
import fr.mtpilates.member.MemberRow;
import fr.mtpilates.member.MemberService;
import fr.mtpilates.shared.ui.FrenchDates;
import fr.mtpilates.shared.web.FormValues;
import fr.mtpilates.user.AppRole;
import fr.mtpilates.user.Profile;
import fr.mtpilates.user.ProfileRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Planifications (programmes) : bibliothèque, éditeur semaines × jours, enregistrement, duplication. */
@Service
public class ProgramService {

    private static final List<String> DAY_TYPES = List.of("workout", "rest", "review", "nutrition", "mixed");

    private final ProgramRepository programs;
    private final WorkoutSessionRepository sessions;
    private final ProfileRepository profiles;
    private final MemberService members;
    private final MemberGuard guard;
    private final ObjectMapper json;

    public ProgramService(final ProgramRepository programs, final WorkoutSessionRepository sessions,
                          final ProfileRepository profiles, final MemberService members, final MemberGuard guard,
                          final ObjectMapper json) {
        this.programs = programs;
        this.sessions = sessions;
        this.profiles = profiles;
        this.members = members;
        this.guard = guard;
        this.json = json;
    }

    @Transactional(readOnly = true)
    public List<ProgramCard> library(final CurrentUser user) {
        ExerciseService.requireStaff(user);
        return programs.findAllByOrderByCreatedAtDesc().stream()
                .map(p -> new ProgramCard(p.getId(), p.getTitle(), p.getDescription(), ProgramStatus.of(p.getStatus()),
                        p.getObjectives(), assigneeLabel(p), period(p)))
                .toList();
    }

    /** Options de l'éditeur : élèves visibles, séances, programmes à recopier. */
    @Transactional(readOnly = true)
    public List<Map<String, String>> memberOptions(final CurrentUser user) {
        return members.visibleTo(user, new MemberFilters("", MemberFilters.ALL, MemberFilters.ALL)).stream()
                .map(m -> option(m.id(), label(m))).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, String>> sessionOptions() {
        return sessions.findAllByOrderByCreatedAtDesc().stream().map(s -> option(s.getId(), s.getTitle())).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, String>> programOptions() {
        return programs.findAllByOrderByCreatedAtDesc().stream().map(p -> option(p.getId(), p.getTitle())).toList();
    }

    /** État de l'éditeur (programToFormPayload) ; une semaine vide pour un nouveau programme. */
    @Transactional(readOnly = true)
    public Map<String, Object> editorState(final CurrentUser user, final Optional<UUID> id, final Optional<UUID> preset) {
        ExerciseService.requireStaff(user);
        return id.flatMap(programs::findById).map(ProgramService::state).orElseGet(() -> blank(preset));
    }

    /** « Pré-remplir depuis un programme existant » : copie non enregistrée, brouillon, sans élève. */
    @Transactional(readOnly = true)
    public Optional<Map<String, Object>> prefill(final CurrentUser user, final UUID sourceId) {
        ExerciseService.requireStaff(user);
        return programs.findById(sourceId).map(p -> {
            final Map<String, Object> state = state(p);
            state.put("title", p.getTitle() + " (copie)");
            state.put("status", "draft");
            state.put("assignee_id", null);
            return state;
        });
    }

    @Transactional
    /** {@code status} : « Brouillon » (draft) ou « Publier » (active), comme les deux boutons de l'original. */
    public UUID save(final CurrentUser user, final Optional<UUID> id, final String payloadJson, final String status) {
        ExerciseService.requireStaff(user);
        final ProgramPayload payload = parse(payloadJson);
        final String title = Optional.ofNullable(payload.title()).map(String::trim).orElse("");
        if (title.isEmpty()) {
            throw new SessionValidationException("Titre requis");
        }
        final Optional<UUID> assignee = uuid(payload.assigneeId());
        requireWritable(user, assignee);
        final Program program = id.flatMap(programs::findById).orElseGet(() -> new Program(user.id()));
        program.getAssigneeId().ifPresent(current -> requireWritable(user, Optional.of(current)));
        final List<ProgramPayload.Week> weeks = Optional.ofNullable(payload.weeks()).orElse(List.of());
        program.describe(payload.title(), payload.description(),
                ProgramStatus.fromCode(status).map(ProgramStatus::code).orElse("draft"), assignee.orElse(null),
                FormValues.date(payload.startDate()).orElse(null), weeks.size(),
                Optional.ofNullable(payload.objectives()).orElse(List.of()), user.id());
        program.clearWeeks();
        programs.saveAndFlush(program);
        IntStream.range(0, weeks.size()).forEach(i -> addWeek(program, i + 1, weeks.get(i)));
        programs.save(program);
        return program.getId();
    }

    @Transactional
    public UUID duplicate(final CurrentUser user, final UUID id) {
        if (!user.realRoles().contains(AppRole.OWNER) && !user.realRoles().contains(AppRole.COACH)) {
            throw new AccessDeniedException("Réservé à l'owner et aux coachs");
        }
        final Program source = programs.findById(id).orElseThrow(() -> new SessionValidationException("Programme introuvable"));
        return programs.save(source.duplicate(user.id())).getId();
    }

    @Transactional
    public void delete(final CurrentUser user, final UUID id) {
        ExerciseService.requireStaff(user);
        programs.findById(id).ifPresent(p -> {
            requireWritable(user, p.getAssigneeId());
            programs.delete(p);
        });
    }

    /** Écriture : staff, et programme commun ou élève suivi par l'utilisateur (policy RLS de programs). */
    private void requireWritable(final CurrentUser user, final Optional<UUID> assignee) {
        if (assignee.isPresent() && !guard.canManage(user, assignee.get())) {
            throw new AccessDeniedException("Élève hors de portée");
        }
    }

    private void addWeek(final Program program, final int number, final ProgramPayload.Week payload) {
        final ProgramWeek week = program.addWeek(number, Optional.ofNullable(payload.comment()).filter(s -> !s.isEmpty()).orElse(null));
        for (final ProgramPayload.Day day : Optional.ofNullable(payload.days()).orElse(List.of())) {
            if (day.dayOfWeek() < 1 || day.dayOfWeek() > 7) {
                continue;
            }
            final List<WorkoutSession> daySessions = Optional.ofNullable(day.sessionIds()).orElse(List.of()).stream()
                    .map(ProgramService::uuid).flatMap(Optional::stream).map(sessions::findById).flatMap(Optional::stream)
                    .toList();
            week.addDay(day.dayOfWeek(), day.dayType() != null && DAY_TYPES.contains(day.dayType()) ? day.dayType() : "rest",
                    Optional.ofNullable(day.comment()).filter(s -> !s.isEmpty()).orElse(null),
                    daySessions.isEmpty() ? null : daySessions.getFirst(), daySessions);
        }
    }

    private String assigneeLabel(final Program p) {
        return p.getAssigneeId().flatMap(profiles::findById).map(ProgramService::nameOrEmail).orElse("Commun à tous");
    }

    private static String period(final Program p) {
        final Optional<Integer> weeks = p.getDurationWeeks().filter(w -> w > 0);
        return p.getStartDate().map(start -> FrenchDates.medium(start) + weeks.map(w -> " · " + w + " sem.").orElse(""))
                .orElseGet(() -> weeks.map(w -> w + " sem.").orElse("—"));
    }

    private static String nameOrEmail(final Profile p) {
        final String name = (p.getFirstName().orElse("") + " " + p.getLastName().orElse("")).trim();
        return name.isEmpty() ? p.getEmail() : name;
    }

    private static String label(final MemberRow m) {
        return m.name().isBlank() ? m.email() : m.name().trim();
    }

    private static Map<String, String> option(final UUID id, final String label) {
        final Map<String, String> option = new LinkedHashMap<>();
        option.put("id", id.toString());
        option.put("label", label);
        return option;
    }

    private static Map<String, Object> blank(final Optional<UUID> preset) {
        final Map<String, Object> state = new LinkedHashMap<>();
        state.put("title", "");
        state.put("description", "");
        state.put("status", "draft");
        state.put("assignee_id", preset.map(UUID::toString).orElse(null));
        state.put("start_date", null);
        state.put("objectives", List.of());
        state.put("weeks", List.of(emptyWeek()));
        return state;
    }

    private static Map<String, Object> emptyWeek() {
        final Map<String, Object> week = new LinkedHashMap<>();
        week.put("week_comment", null);
        week.put("days", IntStream.rangeClosed(1, 7).mapToObj(ProgramService::emptyDay).toList());
        return week;
    }

    private static Map<String, Object> emptyDay(final int dayOfWeek) {
        final Map<String, Object> day = new LinkedHashMap<>();
        day.put("day_of_week", dayOfWeek);
        day.put("day_type", "rest");
        day.put("day_comment", null);
        day.put("workout_session_ids", List.of());
        return day;
    }

    private static Map<String, Object> state(final Program p) {
        final Map<String, Object> state = new LinkedHashMap<>();
        state.put("title", p.getTitle());
        state.put("description", p.getDescription().orElse(""));
        state.put("status", p.getStatus());
        state.put("assignee_id", p.getAssigneeId().map(UUID::toString).orElse(null));
        state.put("start_date", p.getStartDate().map(Object::toString).orElse(null));
        state.put("objectives", p.getObjectives());
        final List<Map<String, Object>> weeks = new ArrayList<>();
        for (final ProgramWeek w : p.getWeeks()) {
            final Map<String, Object> week = new LinkedHashMap<>();
            week.put("week_comment", w.getComment().orElse(null));
            week.put("days", IntStream.rangeClosed(1, 7).mapToObj(n -> w.getDays().stream()
                    .filter(d -> d.getDayOfWeek() == n).findFirst().map(ProgramService::dayState)
                    .orElseGet(() -> emptyDay(n))).toList());
            weeks.add(week);
        }
        state.put("weeks", weeks);
        return state;
    }

    private static Map<String, Object> dayState(final ProgramDay d) {
        final Map<String, Object> day = new LinkedHashMap<>();
        day.put("day_of_week", d.getDayOfWeek());
        day.put("day_type", d.getDayType());
        day.put("day_comment", d.getComment().orElse(null));
        day.put("workout_session_ids", d.getSessions().stream().map(s -> s.getId().toString()).toList());
        return day;
    }

    private ProgramPayload parse(final String payloadJson) {
        try {
            return json.readValue(Optional.ofNullable(payloadJson).orElse("{}"), ProgramPayload.class);
        } catch (final JsonProcessingException e) {
            throw new SessionValidationException("Planification invalide");
        }
    }

    private static Optional<UUID> uuid(final String value) {
        try {
            return value == null || value.isBlank() ? Optional.empty() : Optional.of(UUID.fromString(value));
        } catch (final IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
