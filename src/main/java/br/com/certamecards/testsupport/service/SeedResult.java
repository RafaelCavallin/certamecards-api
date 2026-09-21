package br.com.certamecards.testsupport.service;

import java.util.List;
import java.util.UUID;

public record SeedResult(UUID deckId, List<UUID> cardIds) {}
