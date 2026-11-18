package fr.mtpilates.bilan;

/** Réglages envoyés par l'éditeur : période, poids (texte saisi) et contenu complet en JSON. */
public record BilanSettingsForm(String periodStart, String periodEnd, String targetWeight, String startWeight, String content) {
}
