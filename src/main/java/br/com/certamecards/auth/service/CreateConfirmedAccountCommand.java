package br.com.certamecards.auth.service;

import java.time.Instant;

public record CreateConfirmedAccountCommand(
        String email, String password, String displayName, String timeZone, String termsVersion, Instant now) {}
