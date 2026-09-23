package br.com.certamecards.errorreport.service;

import br.com.certamecards.auditlog.domain.AuditAction;
import br.com.certamecards.auditlog.domain.AuditChange;
import br.com.certamecards.auditlog.domain.AuditTargetType;
import br.com.certamecards.auditlog.service.AdminAuditLogger;
import br.com.certamecards.errorreport.domain.ErrorReportLimits;
import br.com.certamecards.errorreport.domain.ErrorReportStatus;
import br.com.certamecards.errorreport.domain.OpenReportRecord;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ErrorReportAuditRecorder {

    private final AdminAuditLogger auditLogger;

    public ErrorReportAuditRecorder(AdminAuditLogger auditLogger) {
        this.auditLogger = auditLogger;
    }

    public void record(UUID actorId, OpenReportRecord report, ErrorReportStatus outcome) {
        auditLogger.log(
                actorId,
                actionOf(outcome),
                AuditTargetType.ERROR_REPORT,
                report.id(),
                labelOf(report.cardFront()),
                Map.of("status", new AuditChange(ErrorReportStatus.OPEN.code(), outcome.code())));
    }

    private AuditAction actionOf(ErrorReportStatus outcome) {
        return outcome == ErrorReportStatus.RESOLVED
                ? AuditAction.ERROR_REPORT_RESOLVED
                : AuditAction.ERROR_REPORT_REJECTED;
    }

    private String labelOf(String cardFront) {
        int max = ErrorReportLimits.AUDIT_LABEL_MAX_LENGTH;
        return cardFront.length() <= max ? cardFront : cardFront.substring(0, max);
    }
}
