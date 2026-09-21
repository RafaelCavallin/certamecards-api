package br.com.certamecards.review.domain;

import java.util.UUID;

public record AppliedState(UUID cardId, long changeSeq) {}
