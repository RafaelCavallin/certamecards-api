package br.com.certamecards.auditlog.web;

import br.com.certamecards.auditlog.domain.AuditAction;
import br.com.certamecards.auditlog.domain.AuditLogCursor;
import br.com.certamecards.auditlog.domain.AuditLogFilter;
import br.com.certamecards.auditlog.service.AuditLogQueryService;
import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.common.error.ErrorCode;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuditLogAdminController {

    private static final int DEFAULT_SIZE = 50;
    private static final int MAX_SIZE = 100;

    private final AuditLogQueryService queryService;

    public AuditLogAdminController(AuditLogQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping("/api/admin/audit-logs")
    public AuditLogPageResponse list(
            @RequestParam(required = false) UUID actorId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) String before,
            @RequestParam(required = false, defaultValue = "" + DEFAULT_SIZE) int size) {
        AuditLogFilter filter = new AuditLogFilter(
                actorId, parseAction(action), from, to, AuditLogCursor.decode(before), boundedSize(size));
        return AuditLogPageResponse.from(queryService.search(filter));
    }

    private AuditAction parseAction(String action) {
        if (action == null) {
            return null;
        }
        try {
            return AuditAction.fromCode(action);
        } catch (IllegalArgumentException e) {
            throw ApiException.of(ErrorCode.VALIDATION_FAILED);
        }
    }

    private int boundedSize(int size) {
        if (size < 1 || size > MAX_SIZE) {
            throw ApiException.of(ErrorCode.VALIDATION_FAILED);
        }
        return size;
    }
}
