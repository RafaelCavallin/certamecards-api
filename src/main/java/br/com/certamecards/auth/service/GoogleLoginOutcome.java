package br.com.certamecards.auth.service;

import java.util.UUID;

public sealed interface GoogleLoginOutcome {

    record Concluded(UUID userId) implements GoogleLoginOutcome {}

    record LinkRequired(String rawToken) implements GoogleLoginOutcome {}
}
