package br.com.certamecards.sync.domain;

import java.util.List;

public record ConflictListPage(List<ConflictSummary> conflicts, String nextCursor, boolean hasMore) {}
