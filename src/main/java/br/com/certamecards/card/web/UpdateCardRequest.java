package br.com.certamecards.card.web;

import br.com.certamecards.card.domain.CardLimits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCardRequest(
        @NotBlank @Size(max = CardLimits.MAX_FRONT_LENGTH) String front,
        @NotBlank @Size(max = CardLimits.MAX_BACK_LENGTH) String back,
        @Size(max = CardLimits.MAX_SOURCE_LENGTH) String source) {}
