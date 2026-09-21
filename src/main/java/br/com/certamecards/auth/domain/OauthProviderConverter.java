package br.com.certamecards.auth.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class OauthProviderConverter implements AttributeConverter<OauthProvider, String> {

    @Override
    public String convertToDatabaseColumn(OauthProvider attribute) {
        return attribute == null ? null : attribute.code();
    }

    @Override
    public OauthProvider convertToEntityAttribute(String dbData) {
        return dbData == null ? null : OauthProvider.fromCode(dbData);
    }
}
