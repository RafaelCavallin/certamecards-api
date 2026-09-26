package br.com.certamecards.sync.service;

import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.common.error.ErrorCode;
import br.com.certamecards.sync.domain.SyncMutationOperation;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
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

    public ConflictRestorePayload conflictRestore(SyncMutationOperation operation) {
        return read(operation, ConflictRestorePayload.class);
    }

    public DeckMutationPayload deckSnapshot(JsonNode snapshot) {
        return readNode(snapshot, DeckMutationPayload.class);
    }

    public CardMutationPayload cardSnapshot(JsonNode snapshot) {
        return readNode(snapshot, CardMutationPayload.class);
    }

    private <T> T read(SyncMutationOperation operation, Class<T> type) {
        return readNode(operation.payload(), type);
    }

    private <T> T readNode(JsonNode node, Class<T> type) {
        try {
            return objectMapper.treeToValue(node, type);
        } catch (JacksonException exception) {
            throw ApiException.of(ErrorCode.VALIDATION_FAILED);
        }
    }
}
