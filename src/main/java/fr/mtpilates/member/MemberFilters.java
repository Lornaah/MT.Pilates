package fr.mtpilates.member;

/** Filtres de la liste des membres (recherche, coach, statut) ; « all » désactive un filtre. */
public record MemberFilters(String search, String coach, String status) {

    public static final String ALL = "all";

    public MemberFilters {
        search = search == null ? "" : search;
        coach = coach == null || coach.isBlank() ? ALL : coach;
        status = status == null || status.isBlank() ? ALL : status;
    }
}
