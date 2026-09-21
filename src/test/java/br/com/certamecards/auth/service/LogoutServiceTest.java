package br.com.certamecards.auth.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;

class LogoutServiceTest {

    private final RefreshTokenService refreshTokenService = mock(RefreshTokenService.class);
    private final LogoutService service = new LogoutService(refreshTokenService);

    @Test
    void givenRawRefreshToken_whenLoggingOut_thenRevokesThatToken() {
        service.logout("raw-refresh-token");

        verify(refreshTokenService).revokeByRawToken("raw-refresh-token");
    }
}
