package fr.mtpilates.user;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class AppRoleConverter implements AttributeConverter<AppRole, String> {

    @Override
    public String convertToDatabaseColumn(final AppRole role) {
        return role.code();
    }

    @Override
    public AppRole convertToEntityAttribute(final String code) {
        return AppRole.fromCode(code)
                .orElseThrow(() -> new IllegalStateException("Rôle inconnu en base : " + code));
    }
}
