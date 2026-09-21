package br.com.certamecards.auth.web;

public record AuthResponse(String accessToken, long expiresIn, UserResponse user) {}
