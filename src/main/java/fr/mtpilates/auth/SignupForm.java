package fr.mtpilates.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Locale;

/** Formulaire « Créer un compte » (décision Q3 : email et mot de passe seulement). */
public record SignupForm(
        @NotBlank(message = EmailRules.REQUIRED)
        @Email(regexp = EmailRules.REGEX, message = EmailRules.INVALID)
        @Size(max = EmailRules.MAX_LENGTH, message = EmailRules.TOO_LONG)
        String email,
        @StrongPassword
        String password) {

    /** Email tel que zod le normalisait avant validation (trim + minuscules). */
    public SignupForm normalized() {
        return new SignupForm(email == null ? "" : email.trim().toLowerCase(Locale.ROOT), password);
    }
}
