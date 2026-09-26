package br.com.certamecards.sync.service;

import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.common.error.ErrorCode;
import br.com.certamecards.common.sync.EventOrder;
import br.com.certamecards.sync.domain.MutationReceipt;
import br.com.certamecards.sync.persistence.MutationReceiptRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class MutationReceiptService {

    private final MutationReceiptRepository repository;

    public MutationReceiptService(MutationReceiptRepository repository) {
        this.repository = repository;
    }

    public Optional<MutationReceipt> find(UUID userId, UUID operationId) {
        return repository.find(userId, operationId);
    }

    public void ensureSameRequest(MutationReceipt receipt, String requestHash) {
        existingOrReject(receipt, requestHash);
    }

    public MutationReceipt reserve(
            UUID userId,
            UUID operationId,
            String requestHash,
            String kind,
            EventOrder order,
            String outcome,
            Integer entityVersion,
            Long changeSeq,
            UUID conflictId,
            String errorCode,
            Instant createdAt) {
        Optional<MutationReceipt> existing = repository.find(userId, operationId);
        if (existing.isPresent()) {
            return existingOrReject(existing.get(), requestHash);
        }
        MutationReceipt receipt = new MutationReceipt(
                userId,
                operationId,
                requestHash,
                kind,
                order,
                outcome,
                entityVersion,
                changeSeq,
                conflictId,
                errorCode,
                createdAt);
        repository.insert(receipt);
        return repository.find(userId, operationId).orElse(receipt);
    }

    private MutationReceipt existingOrReject(MutationReceipt existing, String requestHash) {
        if (!existing.requestHash().equals(requestHash)) {
            throw ApiException.of(ErrorCode.OPERATION_ID_REUSED);
        }
        return existing;
    }
}
