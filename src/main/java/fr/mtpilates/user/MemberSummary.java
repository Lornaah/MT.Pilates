package fr.mtpilates.user;

import java.util.List;

/** Bloc « Mon résumé » (MemberSummaryForm) : partagé entre l'onboarding, Ma fiche et la fiche coach. */
public record MemberSummary(
        List<String> objectives,
        List<String> allergies,
        List<String> pathologies,
        String workSedentarity,
        String bodyType,
        String sportHistory,
        String coachingMotivation,
        String externalSummary) {

    public MemberSummary {
        objectives = objectives == null ? List.of() : List.copyOf(objectives);
        allergies = allergies == null ? List.of() : List.copyOf(allergies);
        pathologies = pathologies == null ? List.of() : List.copyOf(pathologies);
    }
}
