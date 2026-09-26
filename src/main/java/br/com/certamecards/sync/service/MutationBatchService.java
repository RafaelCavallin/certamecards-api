package br.com.certamecards.sync.service;

import br.com.certamecards.common.sync.EventOrder;
import br.com.certamecards.common.sync.EventOrderNormalizer;
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
    private final PreferenceMutationCoordinator preferenceCoordinator;
    private final MutationHandlerRegistry handlers;
    private final MutationReceiptService receipts;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final SyncMutationMetricsRecorder metrics;

    public MutationBatchService(
            MutationBatchValidator validator,
            MutationDependencyResolver dependencyResolver,
            EventOrderNormalizer orderNormalizer,
            EntityMutationCoordinator coordinator,
            PreferenceMutationCoordinator preferenceCoordinator,
            MutationHandlerRegistry handlers,
            MutationReceiptService receipts,
            ObjectMapper objectMapper,
            Clock clock,
            SyncMutationMetricsRecorder metrics) {
        this.validator = validator;
        this.dependencyResolver = dependencyResolver;
        this.orderNormalizer = orderNormalizer;
        this.coordinator = coordinator;
        this.preferenceCoordinator = preferenceCoordinator;
        this.handlers = handlers;
        this.receipts = receipts;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.metrics = metrics;
    }

    public SyncMutationResponse apply(UUID userId, SyncMutationBatch batch) {
        validator.validate(batch);
        Instant startedAt = clock.instant();
        Instant receivedAt = startedAt;
        Map<UUID, MutationResult> results = new HashMap<>();
        dependencyResolver
                .order(batch.operations())
                .forEach(operation -> results.put(
                        operation.operationId(), applyOne(userId, batch.deviceId(), operation, receivedAt, results)));
        List<MutationResult> ordered = resultsInRequestOrder(batch.operations(), results);
        metrics.record(batch.operations(), ordered, receivedAt);
        metrics.recordBatchDuration(java.time.Duration.between(startedAt, clock.instant()));
        return new SyncMutationResponse(receivedAt, ordered);
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
            EntityMutationCommand command =
                    new EntityMutationCommand(userId, operation, order, handler, hash(operation));
            if (!handlers.needsEntityHead(operation)) return handlePreference(command);
            return coordinator.execute(command);
        } catch (br.com.certamecards.common.error.ApiException exception) {
            return rejected(operation, order, exception.getErrorCode().code());
        } catch (IllegalArgumentException exception) {
            return rejected(operation, order, "validation_failed");
        }
    }

    private MutationResult handlePreference(EntityMutationCommand command) {
        var existing = receipts.find(command.userId(), command.operation().operationId());
        if (existing.isPresent()) return duplicate(existing.get(), command.requestHash());
        return preferenceCoordinator.attempt(command);
    }

    private MutationResult duplicate(br.com.certamecards.sync.domain.MutationReceipt receipt, String requestHash) {
        receipts.ensureSameRequest(receipt, requestHash);
        return new MutationResult(
                receipt.operationId(),
                receipt.replayOutcome(),
                receipt.entityVersion(),
                receipt.changeSeq(),
                receipt.order(),
                receipt.conflictId(),
                receipt.errorCode() == null
                        ? null
                        : new br.com.certamecards.sync.domain.MutationError(
                                receipt.errorCode(), "A alteração foi recusada."));
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
