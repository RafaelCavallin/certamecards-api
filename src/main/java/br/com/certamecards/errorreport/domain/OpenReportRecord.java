package br.com.certamecards.errorreport.domain;

import java.time.Instant;
import java.util.UUID;

public record OpenReportRecord(UUID id, String cardFront, Instant createdAt) {}
