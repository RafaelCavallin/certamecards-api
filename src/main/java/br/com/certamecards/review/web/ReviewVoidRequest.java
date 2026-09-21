package br.com.certamecards.review.web;

import br.com.certamecards.review.service.ReviewVoidInput;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record ReviewVoidRequest(@NotNull UUID reviewId, @NotNull Instant voidedAt) {

    public ReviewVoidInput toInput() {
        return new ReviewVoidInput(reviewId, voidedAt);
    }
}
