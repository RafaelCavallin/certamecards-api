package br.com.certamecards.errorreport.service;

import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.common.error.ErrorCode;
import br.com.certamecards.errorreport.domain.ErrorReport;
import br.com.certamecards.errorreport.domain.ErrorReportStatus;
import br.com.certamecards.errorreport.persistence.ErrorReportStore;
import br.com.certamecards.errorreport.persistence.ReportableCardQuery;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ErrorReportService {

    private static final String SUBMITTED_METRIC = "error_report.submitted";
    private static final String REASON_TAG = "reason";

    private final ReportableCardQuery reportableCards;
    private final ErrorReportStore store;
    private final MeterRegistry meterRegistry;
    private final Clock clock;

    public ErrorReportService(
            ReportableCardQuery reportableCards, ErrorReportStore store, MeterRegistry meterRegistry, Clock clock) {
        this.reportableCards = reportableCards;
        this.store = store;
        this.meterRegistry = meterRegistry;
        this.clock = clock;
    }

    @Transactional
    public ErrorReport submit(SubmitErrorReportCommand command) {
        if (!reportableCards.isReportable(command.userId(), command.cardId())) {
            throw ApiException.of(ErrorCode.NOT_FOUND);
        }
        ErrorReport report = new ErrorReport(
                UUID.randomUUID(),
                command.cardId(),
                command.reason(),
                command.note(),
                ErrorReportStatus.OPEN,
                clock.instant());
        if (!store.insert(report, command.userId())) {
            throw ApiException.of(ErrorCode.REPORT_ALREADY_SENT);
        }
        meterRegistry
                .counter(SUBMITTED_METRIC, REASON_TAG, command.reason().code())
                .increment();
        return report;
    }
}
