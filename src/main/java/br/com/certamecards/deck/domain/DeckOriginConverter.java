package br.com.certamecards.deck.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class DeckOriginConverter implements AttributeConverter<DeckOrigin, String> {

    @Override
    public String convertToDatabaseColumn(DeckOrigin attribute) {
        return attribute == null ? null : attribute.code();
    }

    @Override
    public DeckOrigin convertToEntityAttribute(String dbData) {
        return dbData == null ? null : DeckOrigin.fromCode(dbData);
    }
}
