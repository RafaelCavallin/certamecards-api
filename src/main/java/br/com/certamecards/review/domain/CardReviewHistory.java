package br.com.certamecards.review.domain;

import java.util.List;

public record CardReviewHistory(List<ReviewLogEntry> reviewLogs, List<ReviewVoidEntry> reviewVoids) {}
