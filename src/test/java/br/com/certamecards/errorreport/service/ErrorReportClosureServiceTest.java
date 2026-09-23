package br.com.certamecards.errorreport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.common.error.ErrorCode;
import br.com.certamecards.errorreport.domain.ErrorReportStatus;
import br.com.certamecards.errorreport.domain.OpenReportRecord;
import br.com.certamecards.errorreport.persistence.ErrorReportStore;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ErrorReportClosureServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-21T12:00:00Z");

    private final ErrorReportStore store = mock(ErrorReportStore.class);
    private final ErrorReportAuditRecorder auditRecorder = mock(ErrorReportAuditRecorder.class);
    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final ErrorReportClosureService service = new ErrorReportClosureService(
            store, auditRecorder, new ErrorReportClosureMetrics(meterRegistry), Clock.fixed(NOW, ZoneOffset.UTC));
    private final UUID actorId = UUID.randomUUID();
    private final UUID reportId = UUID.randomUUID();

    @Test
    void givenOpenReport_whenClosing_thenUpdatesAuditsAndRecordsOpenAge() {
        OpenReportRecord report = new OpenReportRecord(reportId, "Frente", NOW.minusSeconds(3L * 86_400));
        when(store.lockOpen(reportId)).thenReturn(Optional.of(report));

        service.close(actorId, reportId, ErrorReportStatus.RESOLVED);

        verify(store).close(reportId, "resolved", actorId, NOW);
        verify(auditRecorder).record(actorId, report, ErrorReportStatus.RESOLVED);
        assertThat(meterRegistry
                        .counter("error_report.closed", "outcome", "resolved")
                        .count())
                .isEqualTo(1.0);
        assertThat(meterRegistry.summary("error_report.open_age_days").totalAmount())
                .isEqualTo(3.0);
    }

    @Test
    void givenAlreadyClosedReport_whenClosing_thenConflict() {
        when(store.lockOpen(reportId)).thenReturn(Optional.empty());
        when(store.exists(reportId)).thenReturn(true);

        assertThatThrownBy(() -> service.close(actorId, reportId, ErrorReportStatus.REJECTED))
                .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.getErrorCode())
                        .isEqualTo(ErrorCode.REPORT_ALREADY_CLOSED));
        verify(store, never()).close(reportId, "rejected", actorId, NOW);
    }

    @Test
    void givenUnknownReport_whenClosing_thenNotFound() {
        when(store.lockOpen(reportId)).thenReturn(Optional.empty());
        when(store.exists(reportId)).thenReturn(false);

        assertThatThrownBy(() -> service.close(actorId, reportId, ErrorReportStatus.REJECTED))
                .isInstanceOfSatisfying(
                        ApiException.class, ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
    }
}
