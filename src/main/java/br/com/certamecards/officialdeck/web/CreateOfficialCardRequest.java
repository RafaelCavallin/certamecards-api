package br.com.certamecards.officialdeck.web;

import br.com.certamecards.card.domain.CardLimits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateOfficialCardRequest(
        @NotNull UUID id,
        @NotBlank String type,
        @NotBlank @Size(max = CardLimits.MAX_FRONT_LENGTH) String front,
        @NotBlank @Size(max = CardLimits.MAX_BACK_LENGTH) String back,
        @Size(max = CardLimits.MAX_SOURCE_LENGTH) String source) {}
