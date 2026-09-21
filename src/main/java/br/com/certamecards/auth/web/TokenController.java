package br.com.certamecards.auth.web;

import br.com.certamecards.auth.service.AccessTokenIssuer;
import br.com.certamecards.auth.service.LogoutService;
import br.com.certamecards.auth.service.RefreshTokenService;
import br.com.certamecards.auth.service.RotationResult;
import br.com.certamecards.user.persistence.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TokenController {

    private final RefreshTokenService refreshTokenService;
    private final AccessTokenIssuer accessTokenIssuer;
    private final LogoutService logoutService;
    private final RefreshCookieFactory cookieFactory;
    private final RefreshCookieReader cookieReader;
    private final UserRepository userRepository;

    public TokenController(
            RefreshTokenService refreshTokenService,
            AccessTokenIssuer accessTokenIssuer,
            LogoutService logoutService,
            RefreshCookieFactory cookieFactory,
            RefreshCookieReader cookieReader,
            UserRepository userRepository) {
        this.refreshTokenService = refreshTokenService;
        this.accessTokenIssuer = accessTokenIssuer;
        this.logoutService = logoutService;
        this.cookieFactory = cookieFactory;
        this.cookieReader = cookieReader;
        this.userRepository = userRepository;
    }

    @PostMapping("/api/auth/refresh")
    public ResponseEntity<AuthResponse> refresh(HttpServletRequest request) {
        String rawToken = cookieReader.read(request);
        RotationResult rotation = refreshTokenService.rotate(rawToken, request.getHeader("User-Agent"));
        var user = userRepository.findById(rotation.userId()).orElseThrow();
        var access = accessTokenIssuer.issue(user.getId());
        AuthResponse body = new AuthResponse(access.token(), access.expiresInSeconds(), UserResponse.from(user));
        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        cookieFactory.issue(rotation.newToken().rawToken()).toString())
                .body(body);
    }

    @PostMapping("/api/auth/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request) {
        String rawToken = cookieReader.read(request);
        logoutService.logout(rawToken);
    }
}
