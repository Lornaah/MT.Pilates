package fr.mtpilates.training;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** État de l'éditeur de planification envoyé en JSON (mêmes clés que programPayloadSchema). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProgramPayload(String title, String description, String status,
                             @JsonProperty("assignee_id") String assigneeId,
                             @JsonProperty("start_date") String startDate, List<String> objectives,
                             List<Week> weeks) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Week(@JsonProperty("week_comment") String comment, List<Day> days) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Day(@JsonProperty("day_of_week") int dayOfWeek, @JsonProperty("day_type") String dayType,
                      @JsonProperty("day_comment") String comment,
                      @JsonProperty("workout_session_ids") List<String> sessionIds) {
    }
}
