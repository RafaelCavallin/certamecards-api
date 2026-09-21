package br.com.certamecards.review.service;

import br.com.certamecards.review.domain.AppliedState;
import br.com.certamecards.review.domain.IgnoredState;
import br.com.certamecards.review.domain.StaleState;
import java.util.List;

public record StateOutcome(List<AppliedState> applied, List<StaleState> stale, List<IgnoredState> ignored) {}
