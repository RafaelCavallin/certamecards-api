package br.com.certamecards.officialdeck.web;

import br.com.certamecards.card.web.CardResponse;
import br.com.certamecards.officialdeck.service.OfficialCardUpdateResult;

public record OfficialCardUpdateResponse(CardResponse card, int affectedSubscribers, boolean contentUpdateQueued) {

    public static OfficialCardUpdateResponse from(OfficialCardUpdateResult result) {
        return new OfficialCardUpdateResponse(
                CardResponse.from(result.card()), result.affectedSubscribers(), result.contentUpdateQueued());
    }
}
