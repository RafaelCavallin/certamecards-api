package br.com.certamecards.auditlog.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.certamecards.auditlog.domain.AdminAuditLog;
import br.com.certamecards.auditlog.domain.AuditAction;
import br.com.certamecards.auditlog.domain.AuditChange;
import br.com.certamecards.auditlog.domain.AuditTargetType;
import br.com.certamecards.auditlog.persistence.AdminAuditLogRepository;
import br.com.certamecards.support.MutableClock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.json.JsonMapper;

class AdminAuditLoggerTest {

    private final AdminAuditLogRepository repository = mock(AdminAuditLogRepository.class);
    private final MutableClock clock = new MutableClock(Instant.parse("2026-09-19T14:05:00Z"), ZoneOffset.UTC);
    private final AdminAuditLogger logger =
            new AdminAuditLogger(repository, JsonMapper.builder().build(), clock);

    @Test
    void givenChanges_whenLogging_thenPersistsEntryWithSerializedChangesAndNoPrivateData() {
        UUID actorId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        when(repository.save(any())).thenAnswer(call -> call.getArgument(0));

        logger.log(
                actorId,
                AuditAction.OFFICIAL_CARD_CONTENT_CHANGED,
                AuditTargetType.OFFICIAL_CARD,
                targetId,
                "CF/88 · Quais são os fundamentos…",
                Map.of("note", new AuditChange(null, "Lei 14.xxx/2026 alterou o prazo.")));

        ArgumentCaptor<AdminAuditLog> captor = ArgumentCaptor.forClass(AdminAuditLog.class);
        org.mockito.Mockito.verify(repository).save(captor.capture());
        AdminAuditLog saved = captor.getValue();
        assertThat(saved.getActorId()).isEqualTo(actorId);
        assertThat(saved.getAction()).isEqualTo("official_card_content_changed");
        assertThat(saved.getTargetType()).isEqualTo("official_card");
        assertThat(saved.getTargetId()).isEqualTo(targetId);
        assertThat(saved.getCreatedAt()).isEqualTo(Instant.parse("2026-09-19T14:05:00Z"));
        assertThat(saved.getChanges()).contains("Lei 14.xxx/2026 alterou o prazo.");
        assertThat(saved.getChanges()).doesNotContain("@");
    }

    @Test
    void givenNoChanges_whenLogging_thenPersistsEmptyChangesObject() {
        when(repository.save(any())).thenAnswer(call -> call.getArgument(0));

        logger.log(
                UUID.randomUUID(),
                AuditAction.OFFICIAL_DECK_DELETED,
                AuditTargetType.OFFICIAL_DECK,
                UUID.randomUUID(),
                "Deck removido",
                null);

        ArgumentCaptor<AdminAuditLog> captor = ArgumentCaptor.forClass(AdminAuditLog.class);
        org.mockito.Mockito.verify(repository).save(captor.capture());
        assertThat(captor.getValue().getChanges()).isEqualTo("{}");
    }
}
