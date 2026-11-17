package fr.mtpilates.training;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.mtpilates.auth.CurrentUser;
import fr.mtpilates.config.ConfigService;
import jakarta.persistence.EntityManager;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Séances d'entraînement : bibliothèque, éditeur par blocs, enregistrement transactionnel, duplication. */
@Service
public class SessionService {

    private static final String DEFAULT_CATEGORIES = "Force,Cardio,Mobilité,HIIT";
    private static final List<String> BLOCK_TYPES = List.of("single", "superset", "triset", "circuit");

    private final WorkoutSessionRepository sessions;
    private final ExerciseRepository exercises;
    private final ConfigService config;
    private final ObjectMapper json;
    private final EntityManager entityManager;

    public SessionService(final WorkoutSessionRepository sessions, final ExerciseRepository exercises,
                          final ConfigService config, final ObjectMapper json, final EntityManager entityManager) {
        this.sessions = sessions;
        this.exercises = exercises;
        this.config = config;
        this.json = json;
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public List<SessionCard> library(final CurrentUser user) {
        ExerciseService.requireStaff(user);
        return sessions.findAllByOrderByCreatedAtDesc().stream().map(SessionService::card).toList();
    }

    @Transactional(readOnly = true)
    public boolean hasExercises() {
        return exercises.count() > 0;
    }

    /** Catégories configurables (session_categories, liste séparée par des virgules). */
    public List<String> categories() {
        final String raw = Optional.ofNullable(config.labels().raw("session_categories")).filter(s -> !s.isBlank())
                .orElse(DEFAULT_CATEGORIES);
        return Arrays.stream(raw.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    /** État initial de l'éditeur, au format du payload. */
    @Transactional(readOnly = true)
    public Map<String, Object> editorState(final CurrentUser user, final Optional<UUID> id) {
        ExerciseService.requireStaff(user);
        final Map<String, Object> state = new LinkedHashMap<>();
        final Optional<WorkoutSession> session = id.flatMap(sessions::findById);
        state.put("title", session.map(WorkoutSession::getTitle).orElse(""));
        state.put("description", session.flatMap(WorkoutSession::getDescription).orElse(""));
        state.put("category", session.flatMap(WorkoutSession::getCategory).orElse(null));
        state.put("duration_minutes", session.flatMap(WorkoutSession::getDurationMinutes).orElse(null));
        state.put("level", session.flatMap(WorkoutSession::getLevel).orElse(null));
        state.put("is_template", session.map(WorkoutSession::isTemplate).orElse(false));
        state.put("is_published", session.map(WorkoutSession::isPublished).orElse(true));
        state.put("blocks", session.map(SessionService::blocksState).orElse(List.of()));
        return state;
    }

    /** Exercices proposés dans le sélecteur (tous, avec leurs attributs par défaut). */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> exerciseOptions() {
        return exercises.findAllByOrderByCreatedAtDesc().stream().map(e -> {
            final Map<String, Object> option = new LinkedHashMap<>();
            option.put("id", e.getId().toString());
            option.put("title", e.content().title());
            option.put("attributes", e.content().defaultAttributes());
            return option;
        }).toList();
    }

    /** Port de sessionPayloadSchema + save_workout_session : blocs vides retirés, puis tout ou rien. */
    @Transactional
    public UUID save(final CurrentUser user, final Optional<UUID> id, final String payloadJson) {
        ExerciseService.requireStaff(user);
        final SessionPayload payload = parse(payloadJson);
        final String title = Optional.ofNullable(payload.title()).map(String::trim).orElse("");
        if (title.isEmpty()) {
            throw new SessionValidationException("Le titre est requis");
        }
        final List<SessionPayload.Block> blocks = Optional.ofNullable(payload.blocks()).orElse(List.of()).stream()
                .map(SessionService::withoutEmptySlots).filter(b -> !b.exercises().isEmpty()).toList();
        if (blocks.isEmpty()) {
            throw new SessionValidationException("Ajoutez au moins un exercice valide");
        }
        final WorkoutSession session = id.flatMap(sessions::findById).orElseGet(() -> new WorkoutSession(user.id()));
        session.describe(payload.title(), payload.description(), payload.category(),
                Optional.ofNullable(payload.durationMinutes()).filter(d -> d > 0).orElse(null),
                SessionLevel.fromCode(payload.level()).map(SessionLevel::code).orElse(null), payload.template(),
                !Boolean.FALSE.equals(payload.published()));
        session.clearBlocks();
        sessions.saveAndFlush(session);
        blocks.forEach(b -> addBlock(session, b));
        sessions.save(session);
        return session.getId();
    }

    @Transactional
    public UUID duplicate(final CurrentUser user, final UUID id) {
        ExerciseService.requireStaff(user);
        final WorkoutSession source = sessions.findById(id).orElseThrow(() -> new SessionValidationException("Séance introuvable"));
        return sessions.save(source.duplicate(user.id())).getId();
    }

    @Transactional
    public void delete(final CurrentUser user, final UUID id) {
        ExerciseService.requireStaff(user);
        sessions.deleteById(id);
        entityManager.flush();
    }

    private void addBlock(final WorkoutSession session, final SessionPayload.Block payload) {
        final String type = BLOCK_TYPES.contains(payload.blockType()) ? payload.blockType() : "single";
        final WorkoutBlock block = session.addBlock(type, Math.max(1, Optional.ofNullable(payload.rounds()).orElse(1)),
                Optional.ofNullable(payload.notes()).filter(s -> !s.isEmpty()).orElse(null));
        for (final SessionPayload.Item item : payload.exercises()) {
            final Exercise exercise = exercises.findById(UUID.fromString(item.exerciseId()))
                    .orElseThrow(() -> new SessionValidationException("Exercice introuvable"));
            block.addExercise(exercise, ExerciseAttributes.parse(writeJson(item.attributes())),
                    Math.max(1, Optional.ofNullable(item.sets()).orElse(1)),
                    Optional.ofNullable(item.note()).filter(s -> !s.isEmpty()).orElse(null));
        }
    }

    private static SessionPayload.Block withoutEmptySlots(final SessionPayload.Block block) {
        final List<SessionPayload.Item> items = Optional.ofNullable(block.exercises()).orElse(List.of()).stream()
                .filter(i -> i.exerciseId() != null && isUuid(i.exerciseId())).toList();
        return new SessionPayload.Block(block.blockType(), block.rounds(), block.notes(), items);
    }

    private SessionPayload parse(final String payloadJson) {
        try {
            return json.readValue(Optional.ofNullable(payloadJson).orElse("{}"), SessionPayload.class);
        } catch (final JsonProcessingException e) {
            throw new SessionValidationException("Séance invalide");
        }
    }

    private String writeJson(final Object value) {
        try {
            return json.writeValueAsString(Optional.ofNullable(value).orElse(List.of()));
        } catch (final JsonProcessingException e) {
            return "[]";
        }
    }

    private static boolean isUuid(final String value) {
        try {
            UUID.fromString(value);
            return true;
        } catch (final IllegalArgumentException e) {
            return false;
        }
    }

    private static List<Map<String, Object>> blocksState(final WorkoutSession session) {
        return session.getBlocks().stream().map(b -> {
            final Map<String, Object> block = new LinkedHashMap<>();
            block.put("block_type", b.getBlockType());
            block.put("rounds", b.getRounds());
            block.put("notes", b.getNotes().orElse(null));
            block.put("exercises", b.getExercises().stream().map(e -> {
                final Map<String, Object> item = new LinkedHashMap<>();
                item.put("exercise_id", e.getExercise().getId().toString());
                item.put("attributes", e.getAttributes());
                item.put("sets", e.getSets());
                item.put("exercise_note", e.getNote().orElse(null));
                return item;
            }).toList());
            return block;
        }).toList();
    }

    private static SessionCard card(final WorkoutSession s) {
        return new SessionCard(s.getId(), s.getTitle(), s.getDescription(), s.getCategory(), s.getLevel().orElse(""),
                s.getLevel().flatMap(SessionLevel::fromCode), s.getDurationMinutes(), s.isTemplate(), s.getBlocks().size(),
                s.getBlocks().stream().mapToInt(b -> b.getExercises().size()).sum());
    }
}
