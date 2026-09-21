package br.com.certamecards.review.service;

import br.com.certamecards.review.domain.RejectedReview;
import java.util.List;

public record ReviewValidationOutcome(List<ReviewLogInput> valid, List<RejectedReview> rejected) {}
