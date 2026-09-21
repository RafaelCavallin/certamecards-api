package br.com.certamecards.common.security;

import br.com.certamecards.user.domain.UserRole;

public record AuthSnapshot(UserRole role, boolean termsAccepted) {}
