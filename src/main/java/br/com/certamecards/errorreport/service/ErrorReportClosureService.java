package br.com.certamecards.errorreport.service;

import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.common.error.ErrorCode;
import br.com.certamecards.errorreport.domain.ErrorReportStatus;
import br.com.certamecards.errorreport.domain.OpenReportRecord;
import br.com.certamecards.errorreport.persistence.ErrorReportStore;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ErrorReportClosureService {

    private final ErrorReportStore store;
    private final ErrorReportAuditRecorder auditRecorder;
    private final ErrorReportClosureMetrics metrics;
    private final Clock clock;

    public ErrorReportClosureService(
            ErrorReportStore store,
            ErrorReportAuditRecorder auditRecorder,
            ErrorReportClosureMetrics metrics,
            Clock clock) {
        this.store = store;
        this.auditRecorder = auditRecorder;
        this.metrics = metrics;
        this.clock = clock;
    }

    @Transactional
    public void close(UUID actorId, UUID reportId, ErrorReportStatus outcome) {
        OpenReportRecord report = store.lockOpen(reportId).orElseThrow(() -> notOpen(reportId));
        Instant now = clock.instant();
        store.close(reportId, outcome.code(), actorId, now);
        auditRecorder.record(actorId, report, outcome);
        metrics.record(outcome, report.createdAt(), now);
    }

    private ApiException notOpen(UUID reportId) {
        return ApiException.of(store.exists(reportId) ? ErrorCode.REPORT_ALREADY_CLOSED : ErrorCode.NOT_FOUND);
    }
}
