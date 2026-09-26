package br.com.certamecards.sync.service;

import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.common.error.ErrorCode;
import br.com.certamecards.sync.domain.ConflictCursor;
import br.com.certamecards.sync.domain.ConflictDetail;
import br.com.certamecards.sync.domain.ConflictListPage;
import br.com.certamecards.sync.domain.ConflictSummary;
import br.com.certamecards.sync.domain.SyncConflict;
import br.com.certamecards.sync.persistence.SyncConflictRepository;
import br.com.certamecards.sync.persistence.SyncEntityHeadQuery;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class SyncConflictService {

    private final SyncConflictRepository conflicts;
    private final SyncEntityHeadQuery heads;
    private final Clock clock;

    public SyncConflictService(SyncConflictRepository conflicts, SyncEntityHeadQuery heads, Clock clock) {
        this.conflicts = conflicts;
        this.heads = heads;
        this.clock = clock;
    }

    public ConflictListPage list(UUID userId, String opaqueCursor, int limit) {
        ConflictCursor cursor = decodeCursor(opaqueCursor);
        List<SyncConflict> fetched = conflicts.list(userId, cursor, limit + 1);
        boolean hasMore = fetched.size() > limit;
        List<SyncConflict> page = hasMore ? fetched.subList(0, limit) : fetched;
        List<ConflictSummary> summaries =
                page.stream().map(ConflictSummary::from).toList();
        return new ConflictListPage(summaries, nextCursorOf(page), hasMore);
    }

    public ConflictDetail detail(UUID userId, UUID conflictId) {
        SyncConflict conflict = findOwned(userId, conflictId);
        ensureNotExpired(conflict);
        var head = heads.find(userId, conflict.entityType(), conflict.entityId());
        return ConflictDetail.of(
                conflict,
                head.map(h -> h.version()).orElse(null),
                head.map(h -> h.deleted()).orElse(true));
    }

    public SyncConflict forRestore(UUID userId, UUID conflictId) {
        SyncConflict conflict = findOwned(userId, conflictId);
        ensureNotExpired(conflict);
        return conflict;
    }

    public void markRestored(UUID conflictId, Instant restoredAt) {
        conflicts.markRestored(conflictId, restoredAt);
    }

    private SyncConflict findOwned(UUID userId, UUID conflictId) {
        return conflicts.find(userId, conflictId).orElseThrow(() -> ApiException.of(ErrorCode.NOT_FOUND));
    }

    private void ensureNotExpired(SyncConflict conflict) {
        if (conflict.isExpired(clock.instant())) {
            throw ApiException.of(ErrorCode.CONFLICT_EXPIRED);
        }
    }

    private String nextCursorOf(List<SyncConflict> page) {
        if (page.isEmpty()) {
            return null;
        }
        SyncConflict last = page.get(page.size() - 1);
        return new ConflictCursor(last.expiresAt(), last.id()).encode();
    }

    private ConflictCursor decodeCursor(String opaqueCursor) {
        if (opaqueCursor == null || opaqueCursor.isBlank()) {
            return null;
        }
        try {
            return ConflictCursor.decode(opaqueCursor);
        } catch (RuntimeException ex) {
            throw ApiException.of(ErrorCode.VALIDATION_FAILED);
        }
    }
}
