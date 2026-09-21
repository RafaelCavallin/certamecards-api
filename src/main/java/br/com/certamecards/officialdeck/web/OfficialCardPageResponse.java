package br.com.certamecards.officialdeck.web;

import br.com.certamecards.card.web.CardResponse;
import java.util.List;

public record OfficialCardPageResponse(List<CardResponse> items, int page, int size, long total) {}
