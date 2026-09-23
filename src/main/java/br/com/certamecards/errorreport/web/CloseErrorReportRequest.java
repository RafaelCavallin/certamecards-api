package br.com.certamecards.errorreport.web;

import br.com.certamecards.errorreport.domain.ErrorReportLimits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CloseErrorReportRequest(@NotNull @Pattern(regexp = ErrorReportLimits.OUTCOME_PATTERN) String outcome) {}
