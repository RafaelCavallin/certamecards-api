package br.com.certamecards.auth.web;

import jakarta.validation.constraints.NotBlank;

public record GoogleLinkRequest(@NotBlank String token, @NotBlank String password) {}
