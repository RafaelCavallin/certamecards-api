package br.com.certamecards.sync.service;

import br.com.certamecards.common.sync.EventOrder;
import br.com.certamecards.settings.domain.Theme;
import br.com.certamecards.settings.domain.UpdateSettingsCommand;
import br.com.certamecards.settings.domain.UserSettings;
import br.com.certamecards.settings.service.UserSettingsService;
import br.com.certamecards.sync.domain.MutationOutcome;
import br.com.certamecards.sync.domain.MutationResult;
import br.com.certamecards.sync.domain.SyncMutationOperation;
import br.com.certamecards.sync.persistence.SettingFieldClockRepository;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class SettingsMutationHandler implements SyncMutationHandler {

    private final UserSettingsService settingsService;
    private final MutationPayloadReader payloadReader;
    private final SettingFieldClockRepository clocks;

    public SettingsMutationHandler(
            UserSettingsService settingsService,
            MutationPayloadReader payloadReader,
            SettingFieldClockRepository clocks) {
        this.settingsService = settingsService;
        this.payloadReader = payloadReader;
        this.clocks = clocks;
    }

    @Override
    public MutationResult handle(UUID userId, MutationContext context) {
        SyncMutationOperation operation = context.operation();
        EventOrder order = context.order();
        SettingsPatchPayload patch = payloadReader.settings(operation);
        UserSettings current = settingsService.get(userId);
        UpdateSettingsCommand command = command(userId, patch, order, current);
        UserSettings updated = settingsService.update(userId, command);
        return new MutationResult(
                operation.operationId(), MutationOutcome.APPLIED, null, updated.getChangeSeq(), order, null, null);
    }

    private UpdateSettingsCommand command(
            UUID userId, SettingsPatchPayload patch, EventOrder order, UserSettings current) {
        return new UpdateSettingsCommand(
                integer(userId, "newPerDay", patch.newPerDay(), order, current.getNewPerDay()),
                integer(userId, "reviewsPerDay", patch.reviewsPerDay(), order, current.getReviewsPerDay()),
                integer(userId, "focusMinutes", patch.focusMinutes(), order, current.getFocusMinutes()),
                date(userId, patch, order, current.getExamDate()),
                text(userId, "timeZone", patch.timeZone(), order, current.getTimeZone()),
                theme(userId, patch, order, current.getTheme()));
    }

    private int integer(UUID userId, String field, Integer value, EventOrder order, int current) {
        return value != null && clocks.advance(userId, field, order) ? value : current;
    }

    private LocalDate date(UUID userId, SettingsPatchPayload patch, EventOrder order, LocalDate current) {
        return patch.examDate() != null && clocks.advance(userId, "examDate", order) ? patch.examDate() : current;
    }

    private String text(UUID userId, String field, String value, EventOrder order, String current) {
        return value != null && clocks.advance(userId, field, order) ? value : current;
    }

    private Theme theme(UUID userId, SettingsPatchPayload patch, EventOrder order, Theme current) {
        return patch.theme() != null && clocks.advance(userId, "theme", order)
                ? Theme.fromCode(patch.theme())
                : current;
    }
}
