package br.com.certamecards.review.domain;

import java.time.Instant;
import java.util.UUID;

public record ReviewVoidEntry(UUID reviewId, Instant voidedAt, long changeSeq) {}
