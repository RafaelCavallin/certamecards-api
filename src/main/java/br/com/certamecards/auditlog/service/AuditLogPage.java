package br.com.certamecards.auditlog.service;

import br.com.certamecards.auditlog.domain.AuditLogEntry;
import java.util.List;

public record AuditLogPage(List<AuditLogEntry> items, String nextBefore) {}
