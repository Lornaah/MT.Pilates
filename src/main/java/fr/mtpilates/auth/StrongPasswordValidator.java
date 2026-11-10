package fr.mtpilates.auth;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.List;

public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

    @Override
    public boolean isValid(final String value, final ConstraintValidatorContext context) {
        final List<String> violations = PasswordPolicy.violations(value);
        if (violations.isEmpty()) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        violations.forEach(message -> context.buildConstraintViolationWithTemplate(message).addConstraintViolation());
        return false;
    }
}
