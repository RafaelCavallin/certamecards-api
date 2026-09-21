package br.com.certamecards.user.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateDisplayNameRequest(@NotBlank @Size(min = 1, max = 60) String displayName) {}
