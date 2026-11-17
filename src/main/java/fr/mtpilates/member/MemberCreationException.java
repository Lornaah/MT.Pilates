package fr.mtpilates.member;

/** Création refusée ; le message, en français, est montré tel quel dans un toast. */
public class MemberCreationException extends RuntimeException {

    public MemberCreationException(final String message) {
        super(message);
    }
}
