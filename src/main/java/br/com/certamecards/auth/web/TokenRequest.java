package br.com.certamecards.auth.web;

import jakarta.validation.constraints.NotBlank;

public record TokenRequest(@NotBlank String token) {}
