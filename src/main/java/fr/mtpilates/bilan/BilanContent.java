package fr.mtpilates.bilan;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Réglages et textes d'un bilan, enregistrés en JSON dans `member_reports.content`.
 *
 * @param durations remplacements des durées calculées (`duree_titre`, `duree`, `semaines`) ; absent = calculé
 * @param measures  clés des mensurations cochées (weight_kg…)
 * @param views     vues photo par code (front, side, back)
 * @param texts     textes du bilan (modèle recopié à la création, puis modifié par le coach)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BilanContent(Map<String, String> durations, List<String> measures, Map<String, View> views,
                           Map<String, String> texts) {

    /** Vue photo : incluse ou non, photos avant / après et traits dessinés par le coach sur chacune. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record View(boolean enabled, String before, String after, List<Stroke> beforeStrokes, List<Stroke> afterStrokes) {

        public List<Stroke> beforeStrokesOrEmpty() {
            return beforeStrokes == null ? List.of() : beforeStrokes;
        }

        public List<Stroke> afterStrokesOrEmpty() {
            return afterStrokes == null ? List.of() : afterStrokes;
        }
    }

    /** Trait d'annotation : couleur CSS, épaisseur et points en coordonnées relatives (0 à 1) à la photo. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Stroke(String color, double width, List<List<Double>> points) {
    }

    public Map<String, String> durationsOrEmpty() {
        return durations == null ? Map.of() : durations;
    }

    public List<String> measuresOrEmpty() {
        return measures == null ? List.of() : measures;
    }

    public Map<String, View> viewsOrEmpty() {
        return views == null ? Map.of() : views;
    }

    public Map<String, String> textsOrEmpty() {
        return texts == null ? new LinkedHashMap<>() : texts;
    }
}
