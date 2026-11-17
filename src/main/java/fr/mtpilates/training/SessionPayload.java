package fr.mtpilates.training;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/** État de l'éditeur de séance envoyé en JSON (mêmes clés que le payload de save_workout_session). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionPayload(String title, String description, String category,
                             @JsonProperty("duration_minutes") Integer durationMinutes, String level,
                             @JsonProperty("is_template") boolean template,
                             @JsonProperty("is_published") Boolean published, List<Block> blocks) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Block(@JsonProperty("block_type") String blockType, Integer rounds, String notes,
                        List<Item> exercises) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(@JsonProperty("exercise_id") String exerciseId, List<Map<String, Object>> attributes,
                       Integer sets, @JsonProperty("exercise_note") String note) {
    }
}
