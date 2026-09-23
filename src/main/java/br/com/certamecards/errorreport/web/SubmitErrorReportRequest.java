package br.com.certamecards.errorreport.web;

import br.com.certamecards.errorreport.domain.ErrorReportLimits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SubmitErrorReportRequest(
        @NotNull @Pattern(regexp = ErrorReportLimits.REASON_PATTERN) String reason,
        @Size(max = ErrorReportLimits.NOTE_MAX_LENGTH) String note) {}
