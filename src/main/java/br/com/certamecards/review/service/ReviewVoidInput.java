package br.com.certamecards.review.service;

import java.time.Instant;
import java.util.UUID;

public record ReviewVoidInput(UUID reviewId, Instant voidedAt) {}
