package br.com.certamecards.auth.service;

public record GoogleProfile(String email, String subject, boolean emailVerified, String displayName) {}
