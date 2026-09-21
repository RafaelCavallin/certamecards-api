package br.com.certamecards.auth.service;

public record IssuedAccessToken(String token, long expiresInSeconds) {}
