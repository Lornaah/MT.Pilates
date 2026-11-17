package fr.mtpilates.habit;

/** « Nouvelle habitude » / « Modifier l'habitude ». */
public record HabitForm(String title, String description, String icon, String frequency, String global) {
}
