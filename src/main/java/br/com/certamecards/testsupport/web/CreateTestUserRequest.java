package br.com.certamecards.testsupport.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTestUserRequest(
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 8, max = 128) String password,
        @NotBlank @Size(min = 1, max = 60) String displayName,
        @NotBlank String timeZone) {}
