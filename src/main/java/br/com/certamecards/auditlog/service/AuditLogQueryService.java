package br.com.certamecards.auditlog.service;

import br.com.certamecards.auditlog.domain.AuditLogCursor;
import br.com.certamecards.auditlog.domain.AuditLogEntry;
import br.com.certamecards.auditlog.domain.AuditLogFilter;
import br.com.certamecards.auditlog.persistence.AuditLogQuery;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AuditLogQueryService {

    private final AuditLogQuery auditLogQuery;

    public AuditLogQueryService(AuditLogQuery auditLogQuery) {
        this.auditLogQuery = auditLogQuery;
    }

    public AuditLogPage search(AuditLogFilter filter) {
        AuditLogFilter overFetch = new AuditLogFilter(
                filter.actorId(), filter.action(), filter.from(), filter.to(), filter.before(), filter.size() + 1);
        List<AuditLogEntry> fetched = new ArrayList<>(auditLogQuery.search(overFetch));
        boolean hasMore = fetched.size() > filter.size();
        if (hasMore) {
            fetched.remove(fetched.size() - 1);
        }
        String nextBefore = hasMore ? cursorOf(fetched.get(fetched.size() - 1)) : null;
        return new AuditLogPage(fetched, nextBefore);
    }

    private String cursorOf(AuditLogEntry entry) {
        return new AuditLogCursor(entry.createdAt(), entry.id()).encode();
    }
}
