package br.com.certamecards.library.web;

import br.com.certamecards.library.domain.DuplicateDeckCommand;
import br.com.certamecards.library.domain.DuplicationOptions;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record DuplicateDeckRequest(@NotNull UUID id, Boolean carryProgress, Boolean cancelSubscription) {

    public DuplicateDeckCommand toCommand(UUID userId, UUID sourceDeckId) {
        boolean carry = carryProgress == null || carryProgress;
        boolean cancel = Boolean.TRUE.equals(cancelSubscription);
        return new DuplicateDeckCommand(userId, sourceDeckId, new DuplicationOptions(id, carry, cancel));
    }
}
