package br.com.certamecards.review.domain;

import java.util.UUID;

public record RejectedReview(UUID id, String code) {

    public static RejectedReview of(UUID id, ReviewRejectionCode code) {
        return new RejectedReview(id, code.code());
    }
}
