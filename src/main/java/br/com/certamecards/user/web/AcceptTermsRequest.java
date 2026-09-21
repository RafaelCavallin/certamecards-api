package br.com.certamecards.user.web;

import jakarta.validation.constraints.NotBlank;

public record AcceptTermsRequest(@NotBlank String version) {}
