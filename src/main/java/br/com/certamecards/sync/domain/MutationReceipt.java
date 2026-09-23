package br.com.certamecards.sync.domain;

import java.time.Instant;
import java.util.UUID;

public record MutationReceipt(
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
        Instant createdAt) {}
