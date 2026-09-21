package br.com.certamecards.officialdeck.web;

import br.com.certamecards.card.domain.CardLimits;
import br.com.certamecards.officialdeck.domain.OfficialContentUpdateLimits;
import jakarta.validation.constraints.Size;

public record UpdateOfficialCardRequest(
        @Size(max = CardLimits.MAX_FRONT_LENGTH) String front,
        @Size(max = CardLimits.MAX_BACK_LENGTH) String back,
        @Size(max = CardLimits.MAX_SOURCE_LENGTH) String source,
        Boolean contentChanged,
        @Size(max = OfficialContentUpdateLimits.MAX_NOTE_LENGTH) String note) {}
