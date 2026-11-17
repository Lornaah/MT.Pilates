package fr.mtpilates.training;

/** Séance refusée à l'enregistrement ; le message est montré tel quel dans un toast. */
public class SessionValidationException extends RuntimeException {

    public SessionValidationException(final String message) {
        super(message);
    }
}
