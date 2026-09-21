package br.com.certamecards.events.service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record SubmitEventCommand(UUID id, String name, Map<String, Object> props, Instant occurredAt) {}
