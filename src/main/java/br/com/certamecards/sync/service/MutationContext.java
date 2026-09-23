package br.com.certamecards.sync.service;

import br.com.certamecards.sync.domain.EventOrder;
import br.com.certamecards.sync.domain.SyncMutationOperation;

public record MutationContext(SyncMutationOperation operation, EventOrder order, Integer currentVersion) {}
