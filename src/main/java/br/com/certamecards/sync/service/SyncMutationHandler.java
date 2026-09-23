package br.com.certamecards.sync.service;

import br.com.certamecards.sync.domain.MutationResult;
import java.util.UUID;

public interface SyncMutationHandler {

    MutationResult handle(UUID userId, MutationContext context);
}
