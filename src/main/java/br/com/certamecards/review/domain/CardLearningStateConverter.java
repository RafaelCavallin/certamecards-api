package br.com.certamecards.review.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class CardLearningStateConverter implements AttributeConverter<CardLearningState, Short> {

    @Override
    public Short convertToDatabaseColumn(CardLearningState attribute) {
        return attribute == null ? null : attribute.code();
    }

    @Override
    public CardLearningState convertToEntityAttribute(Short dbData) {
        return dbData == null ? null : CardLearningState.fromCode(dbData);
    }
}
