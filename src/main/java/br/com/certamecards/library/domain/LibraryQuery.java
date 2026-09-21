package br.com.certamecards.library.domain;

import java.util.UUID;

public record LibraryQuery(UUID userId, String text, UUID subjectId, LibraryPageRequest pageRequest) {}
