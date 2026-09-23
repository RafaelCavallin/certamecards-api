package br.com.certamecards.errorreport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.common.error.ErrorCode;
import br.com.certamecards.errorreport.domain.ErrorReport;
import br.com.certamecards.errorreport.domain.ErrorReportReason;
import br.com.certamecards.errorreport.domain.ErrorReportStatus;
import br.com.certamecards.errorreport.persistence.ErrorReportStore;
import br.com.certamecards.errorreport.persistence.ReportableCardQuery;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ErrorReportServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-21T12:00:00Z");

    private final ReportableCardQuery reportableCards = mock(ReportableCardQuery.class);
    private final ErrorReportStore store = mock(ErrorReportStore.class);
    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final ErrorReportService service =
            new ErrorReportService(reportableCards, store, meterRegistry, Clock.fixed(NOW, ZoneOffset.UTC));
    private final UUID userId = UUID.randomUUID();
    private final UUID cardId = UUID.randomUUID();
    private final SubmitErrorReportCommand command =
            new SubmitErrorReportCommand(userId, cardId, ErrorReportReason.TYPO, "Erro de digitação");

    @Test
    void givenFirstReport_whenSubmitting_thenStoresOpenReportAndCountsByReason() {
        when(reportableCards.isReportable(userId, cardId)).thenReturn(true);
        when(store.insert(any(ErrorReport.class), eq(userId))).thenReturn(true);

        ErrorReport report = service.submit(command);

        assertThat(report.status()).isEqualTo(ErrorReportStatus.OPEN);
        assertThat(report.createdAt()).isEqualTo(NOW);
        assertThat(report.note()).isEqualTo("Erro de digitação");
        assertThat(meterRegistry
                        .counter("error_report.submitted", "reason", "typo")
                        .count())
                .isEqualTo(1.0);
    }

    @Test
    void givenSecondReportOfSameUserAndCard_whenSubmitting_thenRejectsWithReportAlreadySent() {
        when(reportableCards.isReportable(userId, cardId)).thenReturn(true);
        when(store.insert(any(ErrorReport.class), eq(userId))).thenReturn(false);

        assertThatThrownBy(() -> service.submit(command))
                .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.getErrorCode())
                        .isEqualTo(ErrorCode.REPORT_ALREADY_SENT));
        assertThat(meterRegistry.getMeters()).isEmpty();
    }

    @Test
    void givenCardNotReportable_whenSubmitting_thenNotFoundWithoutWriting() {
        when(reportableCards.isReportable(userId, cardId)).thenReturn(false);

        assertThatThrownBy(() -> service.submit(command))
                .isInstanceOfSatisfying(
                        ApiException.class, ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
        verify(store, never()).insert(any(), any());
    }
}
