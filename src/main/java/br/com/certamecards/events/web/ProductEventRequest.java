package br.com.certamecards.events.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record ProductEventRequest(
        @NotNull UUID id, @NotBlank String name, Map<String, Object> props, @NotNull Instant occurredAt) {}
