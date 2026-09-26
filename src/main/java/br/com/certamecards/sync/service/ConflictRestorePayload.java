package br.com.certamecards.sync.service;

import java.util.UUID;
import tools.jackson.databind.JsonNode;

public record ConflictRestorePayload(UUID conflictId, JsonNode snapshot, UUID targetDeckId) {}
