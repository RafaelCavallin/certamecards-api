package br.com.certamecards.officialdeck.web;

import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.common.error.ErrorCode;
import br.com.certamecards.officialdeck.domain.OfficialDeckStatus;

final class OfficialDeckStatusParser {

    private OfficialDeckStatusParser() {}

    static OfficialDeckStatus parseOptional(String status) {
        return status == null ? null : parseRequired(status);
    }

    static OfficialDeckStatus parseRequired(String status) {
        try {
            return OfficialDeckStatus.fromCode(status);
        } catch (IllegalArgumentException e) {
            throw ApiException.of(ErrorCode.VALIDATION_FAILED);
        }
    }
}
