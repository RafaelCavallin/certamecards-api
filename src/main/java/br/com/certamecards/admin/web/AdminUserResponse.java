package br.com.certamecards.admin.web;

import br.com.certamecards.user.domain.User;

public record AdminUserResponse(String id, String email, String displayName) {

    public static AdminUserResponse from(User user) {
        return new AdminUserResponse(user.getId().toString(), user.getEmail(), user.getDisplayName());
    }
}
