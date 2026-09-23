package br.com.certamecards.sync.service;

import br.com.certamecards.sync.domain.EventOrder;
import br.com.certamecards.sync.domain.MutationOutcome;
import br.com.certamecards.sync.domain.MutationResult;
import br.com.certamecards.sync.domain.SyncMutationBatch;
import br.com.certamecards.sync.domain.SyncMutationOperation;
import br.com.certamecards.sync.domain.SyncMutationResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
public class MutationBatchService {

    private final MutationBatchValidator validator;
    private final MutationDependencyResolver dependencyResolver;
    private final EventOrderNormalizer orderNormalizer;
    private final EntityMutationCoordinator coordinator;
    private final MutationHandlerRegistry handlers;
    private final MutationReceiptService receipts;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public MutationBatchService(
            MutationBatchValidator validator,
            MutationDependencyResolver dependencyResolver,
            EventOrderNormalizer orderNormalizer,
            EntityMutationCoordinator coordinator,
            MutationHandlerRegistry handlers,
            MutationReceiptService receipts,
            ObjectMapper objectMapper,
            Clock clock) {
        this.validator = validator;
        this.dependencyResolver = dependencyResolver;
        this.orderNormalizer = orderNormalizer;
        this.coordinator = coordinator;
        this.handlers = handlers;
        this.receipts = receipts;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public SyncMutationResponse apply(UUID userId, SyncMutationBatch batch) {
        validator.validate(batch);
        Instant receivedAt = clock.instant();
        Map<UUID, MutationResult> results = new HashMap<>();
        dependencyResolver
                .order(batch.operations())
                .forEach(operation -> results.put(
                        operation.operationId(), applyOne(userId, batch.deviceId(), operation, receivedAt, results)));
        return new SyncMutationResponse(receivedAt, resultsInRequestOrder(batch.operations(), results));
    }

    private MutationResult applyOne(
            UUID userId,
            UUID deviceId,
            SyncMutationOperation operation,
            Instant receivedAt,
            Map<UUID, MutationResult> results) {
        EventOrder order = orderNormalizer.normalize(
                operation.clock(), deviceId, operation.operationId(), operation.observedServerTime(), receivedAt);
        if (hasFailedDependency(operation, results)) return blocked(operation, order);
        return handle(userId, operation, order);
    }

    private MutationResult handle(UUID userId, SyncMutationOperation operation, EventOrder order) {
        try {
            SyncMutationHandler handler = handlers.forOperation(operation);
            if (!handlers.needsEntityHead(operation)) return handlePreference(userId, operation, order, handler);
            return coordinator.execute(new EntityMutationCommand(userId, operation, order, handler, hash(operation)));
        } catch (br.com.certamecards.common.error.ApiException exception) {
            return rejected(operation, order, exception.getErrorCode().code());
        } catch (IllegalArgumentException exception) {
            return rejected(operation, order, "validation_failed");
        }
    }

    private MutationResult handlePreference(
            UUID userId, SyncMutationOperation operation, EventOrder order, SyncMutationHandler handler) {
        String requestHash = hash(operation);
        var existing = receipts.find(userId, operation.operationId());
        if (existing.isPresent()) return duplicate(existing.get(), requestHash);
        MutationResult result = handler.handle(userId, new MutationContext(operation, order, null));
        receipts.reserve(
                userId,
                operation.operationId(),
                requestHash,
                operation.kind().value(),
                order,
                result.outcome().value(),
                result.entityVersion(),
                result.changeSeq(),
                result.conflictId(),
                result.error() == null ? null : result.error().code(),
                clock.instant());
        return result;
    }

    private MutationResult duplicate(br.com.certamecards.sync.domain.MutationReceipt receipt, String requestHash) {
        receipts.ensureSameRequest(receipt, requestHash);
        return new MutationResult(
                receipt.operationId(),
                MutationOutcome.DUPLICATE,
                receipt.entityVersion(),
                receipt.changeSeq(),
                receipt.order(),
                receipt.conflictId(),
                null);
    }

    private MutationResult rejected(SyncMutationOperation operation, EventOrder order, String code) {
        return new MutationResult(
                operation.operationId(),
                MutationOutcome.ACTION_REQUIRED,
                null,
                null,
                order,
                null,
                new br.com.certamecards.sync.domain.MutationError(code, "A alteração precisa de atenção."));
    }

    private boolean hasFailedDependency(SyncMutationOperation operation, Map<UUID, MutationResult> results) {
        return operation.dependsOn().stream().map(results::get).anyMatch(this::failed);
    }

    private boolean failed(MutationResult result) {
        return result != null
                && result.outcome() != MutationOutcome.APPLIED
                && result.outcome() != MutationOutcome.DUPLICATE;
    }

    private MutationResult blocked(SyncMutationOperation operation, EventOrder order) {
        return new MutationResult(
                operation.operationId(),
                MutationOutcome.DEPENDENCY_BLOCKED,
                null,
                null,
                order,
                null,
                new br.com.certamecards.sync.domain.MutationError(
                        "dependency_failed", "A alteração precisa de atenção."));
    }

    private List<MutationResult> resultsInRequestOrder(
            List<SyncMutationOperation> operations, Map<UUID, MutationResult> results) {
        List<MutationResult> ordered = new ArrayList<>();
        operations.forEach(operation -> ordered.add(results.get(operation.operationId())));
        return List.copyOf(ordered);
    }

    private String hash(SyncMutationOperation operation) {
        try {
            return hex(MessageDigest.getInstance("SHA-256").digest(objectMapper.writeValueAsBytes(operation)));
        } catch (NoSuchAlgorithmException | JacksonException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private String hex(byte[] bytes) {
        return new String(bytes, StandardCharsets.ISO_8859_1)
                .chars()
                .mapToObj(value -> String.format("%02x", value))
                .collect(java.util.stream.Collectors.joining());
    }
}
