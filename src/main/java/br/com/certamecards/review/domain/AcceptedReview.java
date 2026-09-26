package br.com.certamecards.review.domain;

import br.com.certamecards.common.sync.EventOrder;
import java.util.UUID;

public record AcceptedReview(UUID id, EventOrder canonicalOrder) {}
