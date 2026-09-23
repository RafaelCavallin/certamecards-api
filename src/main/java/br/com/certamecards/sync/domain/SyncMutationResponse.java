package br.com.certamecards.sync.domain;

import java.time.Instant;
import java.util.List;

public record SyncMutationResponse(Instant serverTime, List<MutationResult> results) {}
