package br.com.certamecards.auth.service;

public record IssuedToken(String rawToken, java.time.Instant expiresAt) {}
