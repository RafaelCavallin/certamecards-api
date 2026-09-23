package br.com.certamecards.sync.service;

import java.util.UUID;

public record DeckMutationPayload(UUID subjectId, String name, String description) {}
