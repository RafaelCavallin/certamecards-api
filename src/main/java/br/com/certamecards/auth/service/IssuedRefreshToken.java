package br.com.certamecards.auth.service;

import br.com.certamecards.auth.domain.RefreshToken;

public record IssuedRefreshToken(String rawToken, RefreshToken entity) {}
