package fr.mtpilates.training;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Attributs par défaut d'un exercice, échangés en JSON avec l'éditeur Alpine et stockés en jsonb. */
public final class ExerciseAttributes {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final TypeReference<List<Map<String, Object>>> LIST = new TypeReference<>() {
    };

    private ExerciseAttributes() {
    }

    /** Ne garde que les types connus, dans la limite de cinq, avec une valeur du bon genre. */
    public static List<Map<String, Object>> parse(final String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return JSON.readValue(json, LIST).stream()
                    .map(ExerciseAttributes::clean)
                    .flatMap(Optional::stream)
                    .limit(ExerciseAttributeType.MAX_PER_EXERCISE)
                    .toList();
        } catch (final JsonProcessingException e) {
            return List.of();
        }
    }

    public static String toJson(final List<Map<String, Object>> attributes) {
        try {
            return JSON.writeValueAsString(attributes);
        } catch (final JsonProcessingException e) {
            return "[]";
        }
    }

    private static Optional<Map<String, Object>> clean(final Map<String, Object> raw) {
        return ExerciseAttributeType.fromCode(String.valueOf(raw.get("type"))).map(type -> {
            final Map<String, Object> attr = new LinkedHashMap<>();
            attr.put("type", type.code());
            attr.put("default", value(type, raw.get("default")));
            type.unit().filter(u -> type == ExerciseAttributeType.WEIGHT || type == ExerciseAttributeType.TIME
                            || type == ExerciseAttributeType.REST)
                    .ifPresent(u -> attr.put("unit", Optional.ofNullable(raw.get("unit")).map(String::valueOf).orElse(u)));
            return attr;
        });
    }

    private static Object value(final ExerciseAttributeType type, final Object raw) {
        return switch (type.kind()) {
            case NUMBER -> raw instanceof Number n ? n : number(Objects.toString(raw, ""));
            case BOOLEAN -> raw instanceof Boolean b ? b : Boolean.parseBoolean(Objects.toString(raw, "false"));
            case STRING -> Objects.toString(raw, "");
            case NONE -> null;
        };
    }

    private static Number number(final String raw) {
        try {
            final double value = Double.parseDouble(raw);
            return value == Math.rint(value) ? (Number) (long) value : (Number) value;
        } catch (final NumberFormatException e) {
            return 0;
        }
    }
}
