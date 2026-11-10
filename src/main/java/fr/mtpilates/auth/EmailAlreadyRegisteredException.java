package fr.mtpilates.auth;

/** Un compte existe déjà pour cet email. */
public class EmailAlreadyRegisteredException extends RuntimeException {

    public EmailAlreadyRegisteredException() {
        super("Un compte existe déjà avec cet email");
    }
}
