package br.com.certamecards.errorreport.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import br.com.certamecards.auditlog.domain.AuditAction;
import br.com.certamecards.auditlog.domain.AuditTargetType;
import br.com.certamecards.auditlog.service.AdminAuditLogger;
import br.com.certamecards.errorreport.domain.ErrorReportStatus;
import br.com.certamecards.errorreport.domain.OpenReportRecord;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ErrorReportAuditRecorderTest {

    private final AdminAuditLogger auditLogger = mock(AdminAuditLogger.class);
    private final ErrorReportAuditRecorder recorder = new ErrorReportAuditRecorder(auditLogger);
    private final UUID actorId = UUID.randomUUID();

    @Test
    void givenResolvedOutcome_whenRecording_thenLogsResolvedAction() {
        OpenReportRecord report = new OpenReportRecord(UUID.randomUUID(), "Frente", Instant.now());

        recorder.record(actorId, report, ErrorReportStatus.RESOLVED);

        verify(auditLogger)
                .log(
                        eq(actorId),
                        eq(AuditAction.ERROR_REPORT_RESOLVED),
                        eq(AuditTargetType.ERROR_REPORT),
                        eq(report.id()),
                        eq("Frente"),
                        any());
    }

    @Test
    void givenRejectedOutcomeAndLongFront_whenRecording_thenLogsRejectedWithTruncatedLabel() {
        OpenReportRecord report = new OpenReportRecord(UUID.randomUUID(), "x".repeat(400), Instant.now());

        recorder.record(actorId, report, ErrorReportStatus.REJECTED);

        verify(auditLogger)
                .log(
                        eq(actorId),
                        eq(AuditAction.ERROR_REPORT_REJECTED),
                        eq(AuditTargetType.ERROR_REPORT),
                        eq(report.id()),
                        eq("x".repeat(160)),
                        any());
    }
}
