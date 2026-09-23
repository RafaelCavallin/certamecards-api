package br.com.certamecards.sync.web;

import br.com.certamecards.common.security.AuthenticatedUser;
import br.com.certamecards.sync.domain.SyncMutationBatch;
import br.com.certamecards.sync.domain.SyncMutationResponse;
import br.com.certamecards.sync.service.MutationBatchService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SyncMutationController {

    private final MutationBatchService mutationBatchService;

    public SyncMutationController(MutationBatchService mutationBatchService) {
        this.mutationBatchService = mutationBatchService;
    }

    @PostMapping("/api/sync/mutations")
    public SyncMutationResponse mutate(
            @AuthenticationPrincipal AuthenticatedUser principal, @Valid @RequestBody SyncMutationBatch batch) {
        return mutationBatchService.apply(principal.id(), batch);
    }
}
