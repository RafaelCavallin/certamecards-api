package br.com.certamecards.sync.service;

import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.common.error.ErrorCode;
import br.com.certamecards.sync.domain.SyncMutationOperation;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
public class MutationPayloadReader {

    private final ObjectMapper objectMapper;

    public MutationPayloadReader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public DeckMutationPayload deck(SyncMutationOperation operation) {
        return read(operation, DeckMutationPayload.class);
    }

    public CardMutationPayload card(SyncMutationOperation operation) {
        return read(operation, CardMutationPayload.class);
    }

    public CardPreferencePayload cardPreference(SyncMutationOperation operation) {
        return read(operation, CardPreferencePayload.class);
    }

    public SettingsPatchPayload settings(SyncMutationOperation operation) {
        return read(operation, SettingsPatchPayload.class);
    }

    public ProfilePatchPayload profile(SyncMutationOperation operation) {
        return read(operation, ProfilePatchPayload.class);
    }

    private <T> T read(SyncMutationOperation operation, Class<T> type) {
        try {
            return objectMapper.treeToValue(operation.payload(), type);
        } catch (JacksonException exception) {
            throw ApiException.of(ErrorCode.VALIDATION_FAILED);
        }
    }
}
