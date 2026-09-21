package br.com.certamecards.auth.service;

import org.springframework.stereotype.Service;

@Service
public class LogoutService {

    private final RefreshTokenService refreshTokenService;

    public LogoutService(RefreshTokenService refreshTokenService) {
        this.refreshTokenService = refreshTokenService;
    }

    public void logout(String rawRefreshToken) {
        refreshTokenService.revokeByRawToken(rawRefreshToken);
    }
}
