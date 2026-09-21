package br.com.certamecards.events.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ProductEventNameConverter implements AttributeConverter<ProductEventName, String> {

    @Override
    public String convertToDatabaseColumn(ProductEventName attribute) {
        return attribute == null ? null : attribute.code();
    }

    @Override
    public ProductEventName convertToEntityAttribute(String dbData) {
        return dbData == null ? null : ProductEventName.fromCode(dbData);
    }
}
