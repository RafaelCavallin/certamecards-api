package br.com.certamecards.officialdeck.domain;

import java.util.UUID;

public record OfficialDeckAdminFilter(OfficialDeckStatus status, UUID subjectId, int page, int size) {}
