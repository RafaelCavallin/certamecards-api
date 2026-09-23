package br.com.certamecards.sync.domain;

import java.util.List;
import java.util.UUID;

public record SyncMutationBatch(UUID deviceId, List<SyncMutationOperation> operations) {}
