package br.com.certamecards.review.service;

import br.com.certamecards.review.domain.CardStateSnapshot;
import java.util.UUID;

public record CardStatePushInput(UUID cardId, CardStateSnapshot snapshot, int reviewCount) {}
