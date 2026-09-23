package br.com.certamecards.sync.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.certamecards.settings.domain.Theme;
import br.com.certamecards.settings.domain.UpdateSettingsCommand;
import br.com.certamecards.settings.domain.UserSettings;
import br.com.certamecards.settings.service.UserSettingsService;
import br.com.certamecards.sync.domain.EventClock;
import br.com.certamecards.sync.domain.EventOrder;
import br.com.certamecards.sync.domain.MutationOutcome;
import br.com.certamecards.sync.domain.MutationResult;
import br.com.certamecards.sync.domain.SyncMutationOperation;
import br.com.certamecards.sync.domain.SyncOperationKind;
import br.com.certamecards.sync.persistence.SettingFieldClockRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

class SettingsMutationHandlerTest {

    private static final Instant NOW = Instant.parse("2026-09-22T12:00:00Z");
    private static final UUID USER_ID = UUID.randomUUID();
    private final UserSettingsService settingsService = mock(UserSettingsService.class);
    private final SettingFieldClockRepository clocks = mock(SettingFieldClockRepository.class);
    private final MutationPayloadReader payloadReader = new MutationPayloadReader(new ObjectMapper());
    private final SettingsMutationHandler handler = new SettingsMutationHandler(settingsService, payloadReader, clocks);

    @Test
    void givenAllFieldsAdvancing_whenHandling_thenAppliesNewValues() {
        UserSettings current = mock(UserSettings.class);
        stubCurrent(current);
        when(settingsService.get(USER_ID)).thenReturn(current);
        when(clocks.advance(eq(USER_ID), any(), any())).thenReturn(true);
        UserSettings updated = mock(UserSettings.class);
        when(updated.getChangeSeq()).thenReturn(11L);
        when(settingsService.update(eq(USER_ID), any())).thenReturn(updated);
        SyncMutationOperation operation = operation(fullPayload());

        MutationResult result = handler.handle(USER_ID, new MutationContext(operation, order(), null));

        assertThat(result.outcome()).isEqualTo(MutationOutcome.APPLIED);
        assertThat(result.changeSeq()).isEqualTo(11L);
        ArgumentCaptor<UpdateSettingsCommand> captor = ArgumentCaptor.forClass(UpdateSettingsCommand.class);
        org.mockito.Mockito.verify(settingsService).update(eq(USER_ID), captor.capture());
        UpdateSettingsCommand command = captor.getValue();
        assertThat(command.newPerDay()).isEqualTo(20);
        assertThat(command.reviewsPerDay()).isEqualTo(200);
        assertThat(command.focusMinutes()).isEqualTo(25);
        assertThat(command.examDate()).isEqualTo(LocalDate.of(2027, 1, 1));
        assertThat(command.timeZone()).isEqualTo("America/Sao_Paulo");
        assertThat(command.theme()).isEqualTo(Theme.DIA);
    }

    @Test
    void givenClockLosesRace_whenHandling_thenKeepsCurrentValues() {
        UserSettings current = mock(UserSettings.class);
        stubCurrent(current);
        when(settingsService.get(USER_ID)).thenReturn(current);
        when(clocks.advance(eq(USER_ID), any(), any())).thenReturn(false);
        UserSettings updated = mock(UserSettings.class);
        when(updated.getChangeSeq()).thenReturn(12L);
        when(settingsService.update(eq(USER_ID), any())).thenReturn(updated);
        SyncMutationOperation operation = operation(fullPayload());

        handler.handle(USER_ID, new MutationContext(operation, order(), null));

        ArgumentCaptor<UpdateSettingsCommand> captor = ArgumentCaptor.forClass(UpdateSettingsCommand.class);
        org.mockito.Mockito.verify(settingsService).update(eq(USER_ID), captor.capture());
        UpdateSettingsCommand command = captor.getValue();
        assertThat(command.newPerDay()).isEqualTo(current.getNewPerDay());
        assertThat(command.examDate()).isEqualTo(current.getExamDate());
        assertThat(command.theme()).isEqualTo(current.getTheme());
    }

    @Test
    void givenEmptyPayload_whenHandling_thenKeepsCurrentValues() {
        UserSettings current = mock(UserSettings.class);
        stubCurrent(current);
        when(settingsService.get(USER_ID)).thenReturn(current);
        UserSettings updated = mock(UserSettings.class);
        when(updated.getChangeSeq()).thenReturn(13L);
        when(settingsService.update(eq(USER_ID), any())).thenReturn(updated);
        SyncMutationOperation operation = operation(new ObjectMapper().createObjectNode());

        handler.handle(USER_ID, new MutationContext(operation, order(), null));

        ArgumentCaptor<UpdateSettingsCommand> captor = ArgumentCaptor.forClass(UpdateSettingsCommand.class);
        org.mockito.Mockito.verify(settingsService).update(eq(USER_ID), captor.capture());
        UpdateSettingsCommand command = captor.getValue();
        assertThat(command.newPerDay()).isEqualTo(current.getNewPerDay());
        assertThat(command.timeZone()).isEqualTo(current.getTimeZone());
    }

    private void stubCurrent(UserSettings current) {
        when(current.getNewPerDay()).thenReturn(10);
        when(current.getReviewsPerDay()).thenReturn(100);
        when(current.getFocusMinutes()).thenReturn(20);
        when(current.getExamDate()).thenReturn(LocalDate.of(2026, 12, 31));
        when(current.getTimeZone()).thenReturn("America/Recife");
        when(current.getTheme()).thenReturn(Theme.NOITE);
    }

    private ObjectNode fullPayload() {
        ObjectNode node = new ObjectMapper().createObjectNode();
        node.put("newPerDay", 20);
        node.put("reviewsPerDay", 200);
        node.put("focusMinutes", 25);
        node.put("examDate", "2027-01-01");
        node.put("timeZone", "America/Sao_Paulo");
        node.put("theme", "dia");
        return node;
    }

    private SyncMutationOperation operation(ObjectNode payload) {
        return new SyncMutationOperation(
                UUID.randomUUID(),
                SyncOperationKind.SETTINGS_PATCH,
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
