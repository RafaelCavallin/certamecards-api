package br.com.certamecards.admin.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record GrantAdminRequest(@NotBlank @Email String email) {}
