package br.com.certamecards.auth.service;

import br.com.certamecards.user.domain.User;

public record LoginResult(User user, IssuedAccessToken accessToken, IssuedRefreshToken refreshToken) {}
