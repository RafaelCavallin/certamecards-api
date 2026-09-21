package br.com.certamecards.auth.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class OneTimeTokenPurposeConverter implements AttributeConverter<OneTimeTokenPurpose, String> {

    @Override
    public String convertToDatabaseColumn(OneTimeTokenPurpose attribute) {
        return attribute == null ? null : attribute.code();
    }

    @Override
    public OneTimeTokenPurpose convertToEntityAttribute(String dbData) {
        return dbData == null ? null : OneTimeTokenPurpose.fromCode(dbData);
    }
}
