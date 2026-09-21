package br.com.certamecards.officialdeck.web;

import jakarta.validation.constraints.NotBlank;

public record ChangeOfficialDeckStatusRequest(@NotBlank String status) {}
