package br.com.certamecards.sync.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.certamecards.common.sync.EventClock;
import br.com.certamecards.common.sync.EventOrder;
import br.com.certamecards.sync.domain.MutationOutcome;
import br.com.certamecards.sync.domain.MutationResult;
import br.com.certamecards.sync.domain.SyncMutationOperation;
import br.com.certamecards.sync.domain.SyncOperationKind;
import br.com.certamecards.sync.persistence.SettingFieldClockRepository;
import br.com.certamecards.user.service.AccountService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

class ProfileMutationHandlerTest {

    private static final Instant NOW = Instant.parse("2026-09-22T12:00:00Z");
    private static final UUID USER_ID = UUID.randomUUID();
    private final AccountService accountService = mock(AccountService.class);
    private final SettingFieldClockRepository clocks = mock(SettingFieldClockRepository.class);
    private final MutationPayloadReader payloadReader = new MutationPayloadReader(new ObjectMapper());
    private final ProfileMutationHandler handler = new ProfileMutationHandler(accountService, payloadReader, clocks);

    @Test
    void givenDisplayNameAndWinningClock_whenHandling_thenUpdatesDisplayName() {
        when(clocks.advance(any(), any(), any())).thenReturn(true);
        SyncMutationOperation operation = operation(payload("Rafael"));

        MutationResult result = handler.handle(USER_ID, new MutationContext(operation, order(), null));

        assertThat(result.outcome()).isEqualTo(MutationOutcome.APPLIED);
        verify(accountService).updateDisplayName(USER_ID, "Rafael");
    }

    @Test
    void givenDisplayNameButLosingClock_whenHandling_thenDoesNotUpdate() {
        when(clocks.advance(any(), any(), any())).thenReturn(false);
        SyncMutationOperation operation = operation(payload("Rafael"));

        MutationResult result = handler.handle(USER_ID, new MutationContext(operation, order(), null));

        assertThat(result.outcome()).isEqualTo(MutationOutcome.APPLIED);
        verify(accountService, never()).updateDisplayName(any(), any());
    }

    @Test
    void givenNoDisplayName_whenHandling_thenDoesNotUpdate() {
        SyncMutationOperation operation = operation(new ObjectMapper().createObjectNode());

        MutationResult result = handler.handle(USER_ID, new MutationContext(operation, order(), null));

        assertThat(result.outcome()).isEqualTo(MutationOutcome.APPLIED);
        verify(accountService, never()).updateDisplayName(any(), any());
    }

    private ObjectNode payload(String displayName) {
        ObjectNode node = new ObjectMapper().createObjectNode();
        node.put("displayName", displayName);
        return node;
    }

    private SyncMutationOperation operation(ObjectNode payload) {
        return new SyncMutationOperation(
                UUID.randomUUID(),
                SyncOperationKind.PROFILE_PATCH,
                UUID.randomUUID(),
                null,
                null,
                null,
                List.of(),
                NOW,
                new EventClock(NOW, 0),
                NOW,
                payload);
    }

    private EventOrder order() {
        return new EventOrder(NOW, 0, UUID.randomUUID(), UUID.randomUUID());
    }
}
