package br.com.certamecards.errorreport.web;

import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.common.error.ErrorCode;
import br.com.certamecards.common.security.AuthenticatedUser;
import br.com.certamecards.errorreport.domain.ErrorReportFilter;
import br.com.certamecards.errorreport.domain.ErrorReportStatus;
import br.com.certamecards.errorreport.persistence.ErrorReportAdminQuery;
import br.com.certamecards.errorreport.service.ErrorReportClosureService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ErrorReportAdminController {

    private static final int MAX_PAGE_SIZE = 100;

    private final ErrorReportAdminQuery query;
    private final ErrorReportClosureService closureService;

    public ErrorReportAdminController(ErrorReportAdminQuery query, ErrorReportClosureService closureService) {
        this.query = query;
        this.closureService = closureService;
    }

    @GetMapping("/api/admin/error-reports")
    public ErrorReportAdminPageResponse list(
            @RequestParam(defaultValue = "open") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        ErrorReportStatus parsed =
                ErrorReportStatus.find(status).orElseThrow(() -> ApiException.of(ErrorCode.VALIDATION_FAILED));
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw ApiException.of(ErrorCode.VALIDATION_FAILED);
        }
        ErrorReportFilter filter = new ErrorReportFilter(parsed, page, size);
        var items = query.search(filter).stream()
                .map(ErrorReportAdminResponse::from)
                .toList();
        return new ErrorReportAdminPageResponse(items, page, size, query.count(filter));
    }

    @PostMapping("/api/admin/error-reports/{id}/closure")
    public ErrorReportAdminResponse close(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID id,
            @Valid @RequestBody CloseErrorReportRequest request) {
        ErrorReportStatus outcome = ErrorReportStatus.find(request.outcome()).orElseThrow();
        closureService.close(principal.id(), id, outcome);
        return query.findById(id).map(ErrorReportAdminResponse::from).orElseThrow();
    }
}
