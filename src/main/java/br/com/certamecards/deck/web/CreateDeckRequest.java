package br.com.certamecards.deck.web;

import br.com.certamecards.deck.domain.DeckLimits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateDeckRequest(
        @NotNull UUID id,
        @NotNull UUID subjectId,
        @NotBlank @Size(max = DeckLimits.MAX_NAME_LENGTH) String name,
        @Size(max = DeckLimits.MAX_DESCRIPTION_LENGTH) String description) {}
