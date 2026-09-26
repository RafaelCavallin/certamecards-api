package br.com.certamecards.review.service;

import br.com.certamecards.review.domain.AcceptedReview;
import br.com.certamecards.review.domain.RejectedReview;
import java.util.List;

public record ReviewInsertOutcome(
        List<ReviewLogInput> valid, List<AcceptedReview> accepted, List<RejectedReview> rejected) {}
