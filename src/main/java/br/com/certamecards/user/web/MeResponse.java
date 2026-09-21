package br.com.certamecards.user.web;

import br.com.certamecards.user.domain.User;

public record MeResponse(String id, String email, String displayName, String role, boolean termsAccepted) {

    public static MeResponse from(User user) {
        return new MeResponse(
                user.getId().toString(),
                user.getEmail(),
                user.getDisplayName(),
                user.getRole().code(),
                user.getTerms().getAcceptedAt() != null);
    }
}
