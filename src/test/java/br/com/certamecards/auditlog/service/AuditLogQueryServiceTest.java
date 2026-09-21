package br.com.certamecards.auditlog.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.certamecards.auditlog.domain.AuditLogEntry;
import br.com.certamecards.auditlog.domain.AuditLogFilter;
import br.com.certamecards.auditlog.persistence.AuditLogQuery;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AuditLogQueryServiceTest {

    private final AuditLogQuery auditLogQuery = mock(AuditLogQuery.class);
    private final AuditLogQueryService service = new AuditLogQueryService(auditLogQuery);

    @Test
    void givenMoreEntriesThanPageSize_whenSearching_thenTrimsLastItemAndReturnsCursor() {
        AuditLogFilter filter = new AuditLogFilter(null, null, null, null, null, 2);
        AuditLogEntry first = entry(3);
        AuditLogEntry second = entry(2);
        AuditLogEntry third = entry(1);
        when(auditLogQuery.search(any())).thenReturn(List.of(first, second, third));

        AuditLogPage page = service.search(filter);

        assertThat(page.items()).containsExactly(first, second);
        assertThat(page.nextBefore()).isNotNull();
    }

    @Test
    void givenExactlyPageSizeEntries_whenSearching_thenReturnsNoNextCursor() {
        AuditLogFilter filter = new AuditLogFilter(null, null, null, null, null, 2);
        when(auditLogQuery.search(any())).thenReturn(List.of(entry(2), entry(1)));

        AuditLogPage page = service.search(filter);

        assertThat(page.items()).hasSize(2);
        assertThat(page.nextBefore()).isNull();
    }

    private AuditLogEntry entry(int minutesAgo) {
        return new AuditLogEntry(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Rafael",
                "subject_created",
                "subject",
                UUID.randomUUID(),
                "Direito Constitucional",
                "{}",
                Instant.parse("2026-09-19T14:05:00Z").minusSeconds(minutesAgo * 60L));
    }
}
