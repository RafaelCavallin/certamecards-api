package br.com.certamecards.sync.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.common.error.ErrorCode;
import br.com.certamecards.sync.domain.SyncConflict;
import br.com.certamecards.sync.persistence.SyncConflictRepository;
import br.com.certamecards.sync.persistence.SyncEntityHeadQuery;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SyncConflictServiceTest {

    private static final Instant CREATED_AT = Instant.parse("2026-08-01T12:00:00Z");
    private static final Instant EXPIRES_AT = CREATED_AT.plusSeconds(30L * 24 * 3600);
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID CONFLICT_ID = UUID.randomUUID();
    private final SyncConflictRepository conflicts = mock(SyncConflictRepository.class);
    private final SyncEntityHeadQuery heads = mock(SyncEntityHeadQuery.class);

    @Test
    void givenConflictBeforeExpiry_whenPreparingRestore_thenReturnsConflict() {
        when(conflicts.find(USER_ID, CONFLICT_ID)).thenReturn(Optional.of(conflict(EXPIRES_AT, null)));
        SyncConflictService service = serviceAt(EXPIRES_AT.minusSeconds(1));

        SyncConflict result = service.forRestore(USER_ID, CONFLICT_ID);

        assertThat(result.id()).isEqualTo(CONFLICT_ID);
    }

    @Test
    void givenConflictAfterExpiry_whenPreparingRestore_thenThrowsConflictExpired() {
        when(conflicts.find(USER_ID, CONFLICT_ID)).thenReturn(Optional.of(conflict(EXPIRES_AT, null)));
        SyncConflictService service = serviceAt(EXPIRES_AT.plusSeconds(1));

        assertThatThrownBy(() -> service.forRestore(USER_ID, CONFLICT_ID))
                .isInstanceOf(ApiException.class)
                .extracting(exception -> ((ApiException) exception).getErrorCode())
                .isEqualTo(ErrorCode.CONFLICT_EXPIRED);
    }

    @Test
    void givenAlreadyMarkedExpired_whenPreparingRestore_thenThrowsConflictExpiredEvenBeforeDeadline() {
        when(conflicts.find(USER_ID, CONFLICT_ID))
                .thenReturn(Optional.of(conflict(EXPIRES_AT, EXPIRES_AT.minusSeconds(3600))));
        SyncConflictService service = serviceAt(EXPIRES_AT.minusSeconds(1));

        assertThatThrownBy(() -> service.forRestore(USER_ID, CONFLICT_ID))
                .isInstanceOf(ApiException.class)
                .extracting(exception -> ((ApiException) exception).getErrorCode())
                .isEqualTo(ErrorCode.CONFLICT_EXPIRED);
    }

    @Test
    void givenConflictOfAnotherUser_whenPreparingRestore_thenThrowsNotFound() {
        when(conflicts.find(USER_ID, CONFLICT_ID)).thenReturn(Optional.empty());
        SyncConflictService service = serviceAt(CREATED_AT);

        assertThatThrownBy(() -> service.forRestore(USER_ID, CONFLICT_ID))
                .isInstanceOf(ApiException.class)
                .extracting(exception -> ((ApiException) exception).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    void givenRestore_whenMarking_thenDelegatesToRepository() {
        SyncConflictService service = serviceAt(CREATED_AT);

        service.markRestored(CONFLICT_ID, CREATED_AT);

        verify(conflicts).markRestored(CONFLICT_ID, CREATED_AT);
    }

    @Test
    void givenExpiredConflict_whenFetchingDetail_thenThrowsConflictExpired() {
        when(conflicts.find(USER_ID, CONFLICT_ID)).thenReturn(Optional.of(conflict(EXPIRES_AT, null)));
        when(heads.find(any(), any(), any())).thenReturn(Optional.empty());
        SyncConflictService service = serviceAt(EXPIRES_AT.plusSeconds(1));

        assertThatThrownBy(() -> service.detail(USER_ID, CONFLICT_ID))
                .isInstanceOf(ApiException.class)
                .extracting(exception -> ((ApiException) exception).getErrorCode())
                .isEqualTo(ErrorCode.CONFLICT_EXPIRED);
    }

    private SyncConflict conflict(Instant expiresAt, Instant expiredAt) {
        return new SyncConflict(
                CONFLICT_ID,
                USER_ID,
                "deck",
                UUID.randomUUID(),
                null,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "concurrent_edit",
                null,
                null,
                expiresAt,
                null,
                expiredAt,
                CREATED_AT);
    }

    private SyncConflictService serviceAt(Instant now) {
        return new SyncConflictService(conflicts, heads, Clock.fixed(now, ZoneOffset.UTC));
    }
}
