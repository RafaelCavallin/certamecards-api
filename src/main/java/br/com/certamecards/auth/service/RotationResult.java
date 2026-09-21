package br.com.certamecards.auth.service;

import java.util.UUID;

public record RotationResult(UUID userId, IssuedRefreshToken newToken) {}
