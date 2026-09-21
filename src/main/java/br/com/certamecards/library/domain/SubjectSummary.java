package br.com.certamecards.library.domain;

import java.util.UUID;

public record SubjectSummary(UUID id, String name, int deckCount) {}
