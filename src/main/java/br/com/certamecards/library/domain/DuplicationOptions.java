package br.com.certamecards.library.domain;

import java.util.UUID;

public record DuplicationOptions(UUID newDeckId, boolean carryProgress, boolean cancelSubscription) {}
