package br.com.certamecards.sync.service;

import br.com.certamecards.sync.domain.EventOrder;
import br.com.certamecards.sync.domain.SyncMutationOperation;
import java.util.UUID;

public record EntityMutationCommand(
        UUID userId,
        SyncMutationOperation operation,
        EventOrder order,
        SyncMutationHandler handler,
        String requestHash) {}
