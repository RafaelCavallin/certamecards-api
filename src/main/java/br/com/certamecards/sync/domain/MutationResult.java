package br.com.certamecards.sync.domain;

import java.util.UUID;

public record MutationResult(
        UUID operationId,
        MutationOutcome outcome,
        Integer entityVersion,
        Long changeSeq,
        EventOrder canonicalOrder,
        UUID conflictId,
        MutationError error) {}
