package br.com.certamecards.auth.service;

public record RegisterCommand(
        String email, String password, String displayName, String acceptedTermsVersion, String timeZone) {}
