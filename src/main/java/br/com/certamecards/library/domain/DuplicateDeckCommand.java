package br.com.certamecards.library.domain;

import java.util.UUID;

public record DuplicateDeckCommand(UUID userId, UUID sourceDeckId, DuplicationOptions options) {}
