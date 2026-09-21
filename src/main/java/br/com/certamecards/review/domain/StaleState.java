package br.com.certamecards.review.domain;

import java.util.UUID;

public record StaleState(UUID cardId, long serverReviewCount) {}
