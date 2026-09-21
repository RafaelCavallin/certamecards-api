package br.com.certamecards.card.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class CardTypeConverter implements AttributeConverter<CardType, String> {

    @Override
    public String convertToDatabaseColumn(CardType attribute) {
        return attribute == null ? null : attribute.code();
    }

    @Override
    public CardType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : CardType.fromCode(dbData);
    }
}
