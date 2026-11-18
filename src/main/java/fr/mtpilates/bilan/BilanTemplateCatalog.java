package fr.mtpilates.bilan;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Modèle de textes du bilan : une section par page du PDF de référence, avec ses champs et leurs textes par défaut.
 * Mise en forme : **gras**, ==surligné==, __souligné__. Les champs « points » contiennent une ligne par point,
 * au format « Titre : texte ». Les variables entre accolades sont remplacées à la génération (voir {@link #VARIABLES}).
 */
public final class BilanTemplateCatalog {

    /** Champ du modèle : clé, libellé dans les formulaires, hauteur de la zone de saisie, texte par défaut. */
    public record Field(String key, String label, int rows, String defaultText) {
    }

    /** Page du PDF et ses champs. */
    public record Section(String id, String title, List<Field> fields) {
    }

    /** Variables disponibles dans les textes, avec leur description. */
    public static final Map<String, String> VARIABLES = variables();

    public static final List<Section> SECTIONS = List.of(
            new Section("couverture", "Couverture", List.of(
                    field("couverture.titre", "Titre", 1, "TON {duree_titre} DE TRANSFORMATION"),
                    field("couverture.intro", "Introduction", 3, "Prends quelques secondes pour réaliser ce que tu viens "
                            + "d'accomplir. En ==**seulement {duree}**==, ton corps a déjà commencé à se transformer de façon "
                            + "remarquable."),
                    field("couverture.encart", "Encart", 4, "Ce bilan n'est pas juste un récapitulatif. C'est la **preuve "
                            + "concrète que tes efforts paient déjà.** Chaque séance, chaque choix, chaque effort compte — et "
                            + "les résultats sont là pour en témoigner."),
                    field("couverture.texte", "Texte", 4, "Ce que tu as accompli en {semaines}, beaucoup n'y arrivent pas "
                            + "en plusieurs mois. Tu as posé les bases d'une transformation durable. **Et ce n'est que le "
                            + "début.**"))),
            new Section("chiffres", "Tes résultats en chiffres", List.of(
                    field("chiffres.titre", "Titre", 1, "TES RÉSULTATS EN CHIFFRES"),
                    field("chiffres.intro", "Introduction", 2, "En seulement quelques semaines d'efforts réguliers, voici ce "
                            + "que ton corps a accompli :"),
                    field("chiffres.poids", "Légende du poids", 2, "De {poids_depart} à {poids_actuel} — une évolution "
                            + "solide et durable."),
                    field("chiffres.taille", "Légende du tour de taille", 2, "Le résultat le plus impressionnant de cette "
                            + "transformation."),
                    field("chiffres.poitrine", "Légende de la poitrine", 2, "Un affinement visible et harmonieux."),
                    field("chiffres.hanches", "Légende des hanches", 2, "Une zone réputée difficile — et pourtant, déjà "
                            + "transformée."),
                    field("chiffres.cuisses", "Légende des cuisses", 2, "Des jambes qui s'affinent et se tonifient."),
                    field("chiffres.bras", "Légende des bras", 2, "Des bras plus fermes et plus dessinés."),
                    field("chiffres.note", "Encart", 3, "Ces résultats sont **énormes pour seulement {duree}.** Beaucoup "
                            + "de personnes mettent 3 à 4 mois pour atteindre ce niveau de transformation. Tu es bien en "
                            + "avance sur le calendrier."))),
            view("dos", "TA TRANSFORMATION VUE DE DOS", "Vue de dos", "Les photos avant/après parlent d'elles-mêmes. Ce que "
                            + "l'on remarque immédiatement sur la vue de dos :",
                    "Taille plus marquée : La silhouette se définit clairement. Le galbe naturel du dos reprend sa forme.\n"
                            + "Poignées d'amour réduites : Une diminution nette et visible sur les côtés du buste — l'un des "
                            + "signes les plus encourageants.\n"
                            + "Dos plus dessiné : Les muscles dorsaux commencent à apparaître. Le travail de tonification "
                            + "porte ses fruits.\n"
                            + "Silhouette harmonieuse : L'ensemble du corps retrouve des proportions plus équilibrées et "
                            + "naturellement harmonieuses.",
                    "Ton corps commence déjà à **se redessiner.** Et ce n'est que le début de cette belle métamorphose."),
            view("profil", "TA TRANSFORMATION DE PROFIL", "Vue de profil", "Sur la vue de profil, la transformation est tout "
                            + "aussi visible et impressionnante. Voici ce qui ressort immédiatement :",
                    "Ventre plus plat : Le travail abdominal commence à porter ses fruits. La zone centrale se renforce et "
                            + "s'aplatit progressivement.\n"
                            + "Hanches affinées : Le profil révèle un affinement net des hanches, donnant une silhouette plus "
                            + "élancée et féminine.\n"
                            + "Meilleure posture : L'entraînement régulier renforce les muscles profonds, ce qui améliore "
                            + "naturellement le port du corps.\n"
                            + "Corps plus tonique : La fermeté musculaire est visible. Ton centre du corps se renforce — et ça "
                            + "se voit clairement.",
                    ""),
            view("face", "TA TRANSFORMATION DE FACE", "Vue de face", "De face aussi, le changement saute aux yeux. Voici ce qui "
                            + "a évolué :",
                    "Taille affinée : La taille se resserre et dessine une silhouette plus marquée.\n"
                            + "Ventre plus ferme : La sangle abdominale se renforce, le ventre s'aplatit.\n"
                            + "Épaules ouvertes : La posture se redresse, le haut du corps gagne en assurance.\n"
                            + "Silhouette équilibrée : Les proportions s'harmonisent du haut vers le bas du corps.",
                    ""),
            new Section("poids", "L'évolution de ton poids", List.of(
                    field("poids.titre", "Titre", 1, "L'ÉVOLUTION DE TON POIDS"),
                    field("poids.texte", "Texte", 4, "Ton poids descend **de ==manière régulière et saine==** — exactement "
                            + "ce que l'on recherche dans une transformation durable. Pas de régime extrême, pas de perte "
                            + "rapide suivie d'une reprise frustrante. Juste **un vrai changement progressif et constant.** "
                            + "C'est cette courbe régulière qui garantit que les résultats se maintiennent dans le temps."))),
            measure("taille", "L'ÉVOLUTION DE TON TOUR DE TAILLE", "de tour de taille en {duree}.",
                    "Le tour de taille est l'un des **meilleurs indicateurs** de ta transformation : il reflète directement "
                            + "le travail de ton centre du corps.",
                    "Une taille qui s'affine, c'est une silhouette qui se redessine. Bravo !", ""),
            measure("poitrine", "L'ÉVOLUTION DE TA POITRINE", "de tour de poitrine en {duree}.",
                    "Le haut du corps s'affine lui aussi, **de façon harmonieuse**, sans perdre en tonicité.", "", ""),
            measure("hanches", "L'ÉVOLUTION DE TES HANCHES", "en seulement quelques semaines de tour de hanches.",
                    "Les hanches sont une zone où le corps met souvent **plus de temps à évoluer**. La graisse s'y installe "
                            + "facilement et résiste aux efforts. Pourtant, chez toi, les résultats sont déjà bien visibles "
                            + "dès ce premier mois.",
                    "C'est une zone réputée difficile — et tu y as déjà fait une différence remarquable. Félicitations !", ""),
            measure("cuisses", "L'ÉVOLUTION DE TES CUISSES", "de tour de cuisses en {duree}.",
                    "Ce résultat est particulièrement significatif. Il traduit trois évolutions concrètes dans ton corps :",
                    "",
                    "Moins de stockage : Le corps puise dans les réserves adipeuses de cette zone.\n"
                            + "Plus de tonicité : Les muscles se raffermissent et se sculptent progressivement.\n"
                            + "Silhouette redessinée : L'ensemble du bas du corps s'affine de manière harmonieuse."),
            measure("bras", "L'ÉVOLUTION DE TES BRAS", "de tour de bras en {duree} — une preuve que la transformation est "
                            + "**globale**.",
                    "Les bras sont souvent une zone négligée dans les bilans de transformation, pourtant ils témoignent ici "
                            + "d'un travail complet. Ton corps ne se contente pas de perdre du poids : il se **raffermit et "
                            + "s'affine partout, de façon homogène.**",
                    "", ""),
            new Section("sens", "Ce que ces résultats veulent dire", List.of(
                    field("sens.titre", "Titre", 1, "__CE QUE CES RÉSULTATS VEULENT DIRE__"),
                    field("sens.intro", "Introduction", 3, "Ces résultats ne sont **__pas dus au hasard__.** Ils sont la "
                            + "==conséquence== directe de choix que tu as faits chaque jour, même __quand c'était difficile.__"),
                    field("sens.points", "Points (une ligne « Titre : texte » par point)", 4, "Ta régularité : Tu as montré à "
                            + "ton corps que tu étais là, jour après jour.\n"
                            + "Tes entraînements : Chaque séance a contribué à remodeler ta silhouette.\n"
                            + "Tes efforts : Tu as choisi de te dépasser, encore et encore.\n"
                            + "Ta discipline : Tu as tenu. Et c'est précisément ça qui change tout."),
                    field("sens.citation", "Citation", 2, "Tu n'as pas juste perdu du poids. Tu as prouvé que tu es capable de "
                            + "tenir tes engagements envers toi-même. C'est une force qui va bien au-delà du physique."))),
            new Section("important", "Mais le plus important", List.of(
                    field("important.titre", "Titre", 1, "MAIS LE PLUS IMPORTANT"),
                    field("important.intro", "Introduction", 3, "Ce que tu vois aujourd'hui… **ce n'est que le début.** Le "
                            + "premier mois joue un rôle fondamental que beaucoup sous-estiment :"),
                    field("important.points", "Étapes (une ligne « Titre : texte » par étape)", 3, "Lancer la transformation : "
                            + "Le corps prend ses premiers repères. Les changements métaboliques s'enclenchent.\n"
                            + "Adapter le corps : Les muscles, les articulations et le système cardiovasculaire s'adaptent "
                            + "progressivement à l'effort.\n"
                            + "Créer les habitudes : C'est ici que se forge la discipline qui rendra la transformation durable "
                            + "et naturelle."),
                    field("important.encart", "Encart", 3, "Les transformations les plus impressionnantes arrivent **dans les "
                            + "mois suivants.** Si tu as déjà obtenu **ça en {semaines}**… Imagine ton corps dans **3 mois.**"))),
            new Section("objectif", "L'évolution vers ton objectif", List.of(
                    field("objectif.titre", "Titre", 1, "L'ÉVOLUTION VERS TON OBJECTIF"),
                    field("objectif.intro", "Introduction", 2, "Tu as commencé ton parcours à **{poids_depart}** avec un "
                            + "objectif clair : atteindre **{poids_objectif}** (soit {kg_a_perdre} à perdre au total)."),
                    field("objectif.situation", "Situation", 2, "Après seulement **{semaines}**, tu es déjà à "
                            + "**{poids_actuel}**. Chaque gramme perdu est un pas de plus vers ton but final !"),
                    field("objectif.progression", "Sous la barre de progression", 2, "En {semaines}, tu as déjà accompli "
                            + "{progression} de ton objectif de perte de poids total ({poids_perdu} perdus sur {kg_a_perdre} "
                            + "à perdre). Un début remarquable !"),
                    field("objectif.conclusion", "Conclusion", 2, "Le chemin est tracé, et tu es en excellente voie. Ton "
                            + "engagement et ta régularité te mèneront à l'atteinte de tes {poids_objectif}."))),
            new Section("fierte", "Sois fière de toi", List.of(
                    field("fierte.titre", "Titre", 1, "SOIS FIÈRE DE TOI"),
                    field("fierte.intro", "Introduction", 3, "Beaucoup de personnes abandonnent avant même d'arriver à ce "
                            + "stade. Toi, tu as choisi de continuer, encore et encore. C'est ce qui fait toute la différence."),
                    field("fierte.points", "Points (une ligne « Titre : texte » par point)", 4, "Tu as commencé : Le premier pas "
                            + "est souvent le plus difficile. Tu l'as franchi.\n"
                            + "Tu as tenu : Malgré les doutes et la fatigue, tu es restée fidèle à ton engagement.\n"
                            + "Tu as progressé : Les chiffres parlent d'eux-mêmes. Tes efforts ont produit de vrais résultats.\n"
                            + "Tu te transformes : Ton corps change. Ta force intérieure aussi. Et ça, ça mérite d'être célébré."),
                    field("fierte.citation", "Citation", 3, "\"__Je suis en train de le faire.__\" Dis-toi ces mots. Crois-y. "
                            + "Parce que c'est exactement la vérité — et ce n'est que le début."),
                    field("fierte.signature", "Signature", 1, "BRAVO."))));

    private BilanTemplateCatalog() {
    }

    /** Textes par défaut, par clé. */
    public static Map<String, String> defaults() {
        final Map<String, String> map = new LinkedHashMap<>();
        SECTIONS.forEach(s -> s.fields().forEach(f -> map.put(f.key(), f.defaultText())));
        return map;
    }

    private static Field field(final String key, final String label, final int rows, final String text) {
        return new Field(key, label, rows, text);
    }

    private static Section view(final String id, final String title, final String sectionTitle, final String intro,
                                final String points, final String conclusion) {
        return new Section("vue." + id, sectionTitle, List.of(
                field("vue." + id + ".titre", "Titre", 1, title),
                field("vue." + id + ".intro", "Introduction", 2, intro),
                field("vue." + id + ".points", "Points (une ligne « Titre : texte » par point)", 5, points),
                field("vue." + id + ".conclusion", "Conclusion", 2, conclusion)));
    }

    private static Section measure(final String id, final String title, final String subtitle, final String text,
                                   final String note, final String points) {
        return new Section("mesure." + id, BilanMetric.fromSlug(id).map(BilanMetric::label).orElse(id), List.of(
                field("mesure." + id + ".titre", "Titre", 1, title),
                field("mesure." + id + ".sous_titre", "Sous le chiffre", 2, subtitle),
                field("mesure." + id + ".texte", "Texte", 4, text),
                field("mesure." + id + ".note", "Encart", 2, note),
                field("mesure." + id + ".points", "Points (une ligne « Titre : texte » par point)", 3, points)));
    }

    private static Map<String, String> variables() {
        final Map<String, String> map = new LinkedHashMap<>();
        map.put("prenom", "Prénom de la membre");
        map.put("duree", "Durée de la période (ex. « 1 mois »)");
        map.put("duree_titre", "Durée pour le titre (ex. « 1er MOIS »)");
        map.put("semaines", "Nombre de semaines (ex. « 4 semaines »)");
        map.put("poids_depart", "Poids de départ (ex. « 67 kg »)");
        map.put("poids_actuel", "Poids actuel (ex. « 63,7 kg »)");
        map.put("poids_objectif", "Poids objectif (ex. « 55 kg »)");
        map.put("poids_perdu", "Poids perdu (ex. « 3,3 kg »)");
        map.put("kg_a_perdre", "Écart départ → objectif (ex. « 12 kg »)");
        map.put("kg_restants", "Reste à perdre (ex. « 8,7 kg »)");
        map.put("progression", "Part de l'objectif atteinte (ex. « 27,5 % »)");
        return java.util.Collections.unmodifiableMap(map);
    }
}
