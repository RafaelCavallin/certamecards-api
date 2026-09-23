package br.com.certamecards.errorreport.service;

import br.com.certamecards.errorreport.domain.ErrorReportReason;
import java.util.UUID;

public record SubmitErrorReportCommand(UUID userId, UUID cardId, ErrorReportReason reason, String note) {}
