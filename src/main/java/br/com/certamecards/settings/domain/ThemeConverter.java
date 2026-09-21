package br.com.certamecards.settings.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ThemeConverter implements AttributeConverter<Theme, String> {

    @Override
    public String convertToDatabaseColumn(Theme attribute) {
        return attribute == null ? null : attribute.code();
    }

    @Override
    public Theme convertToEntityAttribute(String dbData) {
        return dbData == null ? null : Theme.fromCode(dbData);
    }
}
