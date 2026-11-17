package fr.mtpilates.member;

/** « Inscrire un membre » : compte créé par le management avec un mot de passe temporaire. */
public record CreateMemberForm(String email, String password, String firstName, String lastName) {
}
