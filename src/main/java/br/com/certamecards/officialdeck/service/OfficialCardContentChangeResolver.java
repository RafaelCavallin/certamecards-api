package br.com.certamecards.officialdeck.service;

import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.common.error.ApiFieldError;
import br.com.certamecards.common.error.ErrorCode;
import br.com.certamecards.officialdeck.domain.OfficialDeckStatus;
import org.springframework.stereotype.Component;

@Component
public class OfficialCardContentChangeResolver {

    public boolean resolve(OfficialDeckStatus deckStatus, UpdateOfficialCardCommand command) {
        if (deckStatus == OfficialDeckStatus.DRAFT) {
            return Boolean.TRUE.equals(command.contentChanged());
        }
        if (command.contentChanged() == null) {
            throw ApiException.withDetail(ErrorCode.VALIDATION_FAILED, "Informe se o conteúdo mudou.");
        }
        if (command.contentChanged()
                && (command.note() == null || command.note().isBlank())) {
            throw ApiException.withFieldError(
                    ErrorCode.VALIDATION_FAILED,
                    ErrorCode.VALIDATION_FAILED.detail(),
                    new ApiFieldError("note", "required", "Escreva o que mudou no conteúdo."));
        }
        return command.contentChanged();
    }
}
