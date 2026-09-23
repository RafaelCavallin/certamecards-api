package br.com.certamecards.sync.domain;

import java.time.Instant;
import java.util.List;

public record ChangesPage(Instant serverTime, List<ChangeEntry> changes, long nextCursor, boolean hasMore) {}
