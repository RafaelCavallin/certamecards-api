package br.com.certamecards.errorreport.web;

import br.com.certamecards.common.security.AuthenticatedUser;
import br.com.certamecards.errorreport.domain.ErrorReportReason;
import br.com.certamecards.errorreport.service.ErrorReportService;
import br.com.certamecards.errorreport.service.SubmitErrorReportCommand;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ErrorReportController {

    private final ErrorReportService service;

    public ErrorReportController(ErrorReportService service) {
        this.service = service;
    }

    @PostMapping("/api/cards/{id}/error-reports")
    @ResponseStatus(HttpStatus.CREATED)
    public ErrorReportResponse submit(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID id,
            @Valid @RequestBody SubmitErrorReportRequest request) {
        ErrorReportReason reason = ErrorReportReason.fromCode(request.reason());
        var command = new SubmitErrorReportCommand(principal.id(), id, reason, request.note());
        return ErrorReportResponse.from(service.submit(command));
    }
}
