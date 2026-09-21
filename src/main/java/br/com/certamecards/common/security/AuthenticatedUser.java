package br.com.certamecards.common.security;

import br.com.certamecards.user.domain.UserRole;
import java.util.UUID;

public record AuthenticatedUser(UUID id, UserRole role, boolean termsAccepted) {}
