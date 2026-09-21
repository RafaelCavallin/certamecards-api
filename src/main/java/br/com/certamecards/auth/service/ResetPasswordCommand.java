package br.com.certamecards.auth.service;

public record ResetPasswordCommand(String rawToken, String newPassword) {}
