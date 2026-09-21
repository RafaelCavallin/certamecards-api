package br.com.certamecards.auditlog.web;

import br.com.certamecards.auditlog.service.AuditLogPage;
import java.util.List;

public record AuditLogPageResponse(List<AuditLogEntryResponse> items, String nextBefore) {

    public static AuditLogPageResponse from(AuditLogPage page) {
        List<AuditLogEntryResponse> items =
                page.items().stream().map(AuditLogEntryResponse::from).toList();
        return new AuditLogPageResponse(items, page.nextBefore());
    }
}
