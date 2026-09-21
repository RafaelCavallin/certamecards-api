package br.com.certamecards.sync.domain;

import java.time.Instant;
import java.util.UUID;

public record ReviewVoidChange(UUID reviewId, Instant voidedAt, long changeSeq) {}
