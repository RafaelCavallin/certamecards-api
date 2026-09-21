package br.com.certamecards.review.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ReviewKindConverter implements AttributeConverter<ReviewKind, String> {

    @Override
    public String convertToDatabaseColumn(ReviewKind attribute) {
        return attribute == null ? null : attribute.code();
    }

    @Override
    public ReviewKind convertToEntityAttribute(String dbData) {
        return dbData == null ? null : ReviewKind.fromCode(dbData);
    }
}
