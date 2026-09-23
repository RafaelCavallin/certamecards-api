package br.com.certamecards.sync.service;

import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.common.error.ErrorCode;
import br.com.certamecards.sync.domain.SyncMutationOperation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class MutationDependencyResolver {

    public List<SyncMutationOperation> order(List<SyncMutationOperation> operations) {
        Map<UUID, SyncMutationOperation> byId = index(operations);
        List<SyncMutationOperation> ordered = new ArrayList<>();
        Set<UUID> completed = new HashSet<>();
        Set<UUID> visiting = new HashSet<>();
        operations.forEach(operation -> visit(operation, byId, completed, visiting, ordered));
        return ordered;
    }

    private Map<UUID, SyncMutationOperation> index(List<SyncMutationOperation> operations) {
        Map<UUID, SyncMutationOperation> result = new HashMap<>();
        operations.forEach(operation -> {
            if (result.put(operation.operationId(), operation) != null) invalid();
        });
        return result;
    }

    private void visit(
            SyncMutationOperation operation,
            Map<UUID, SyncMutationOperation> byId,
            Set<UUID> completed,
            Set<UUID> visiting,
            List<SyncMutationOperation> ordered) {
        if (completed.contains(operation.operationId())) return;
        if (!visiting.add(operation.operationId())) invalid();
        operation.dependsOn().stream()
                .map(byId::get)
                .filter(java.util.Objects::nonNull)
                .forEach(dependency -> visit(dependency, byId, completed, visiting, ordered));
        visiting.remove(operation.operationId());
        completed.add(operation.operationId());
        ordered.add(operation);
    }

    private void invalid() {
        throw ApiException.of(ErrorCode.VALIDATION_FAILED);
    }
}
