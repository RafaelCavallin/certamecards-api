package br.com.certamecards.officialdeck.web;

import java.util.List;

public record OfficialDeckAdminPageResponse(
        List<OfficialDeckAdminSummaryResponse> items, int page, int size, long total) {}
