package br.com.certamecards.auth.web;

import br.com.certamecards.user.domain.User;

public record UserResponse(String id, String email, String displayName, String role, boolean termsAccepted) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId().toString(),
                user.getEmail(),
                user.getDisplayName(),
                user.getRole().code(),
                user.getTerms().getAcceptedAt() != null);
    }
}
