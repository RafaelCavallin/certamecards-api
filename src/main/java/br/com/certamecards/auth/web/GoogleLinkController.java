package br.com.certamecards.auth.web;

import br.com.certamecards.auth.service.AccessTokenIssuer;
import br.com.certamecards.auth.service.GoogleLinkService;
import br.com.certamecards.auth.service.IssuedAccessToken;
import br.com.certamecards.auth.service.IssuedRefreshToken;
import br.com.certamecards.auth.service.RefreshTokenService;
import br.com.certamecards.user.domain.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GoogleLinkController {

    private final GoogleLinkService googleLinkService;
    private final AccessTokenIssuer accessTokenIssuer;
    private final RefreshTokenService refreshTokenService;
    private final RefreshCookieFactory cookieFactory;

    public GoogleLinkController(
            GoogleLinkService googleLinkService,
            AccessTokenIssuer accessTokenIssuer,
            RefreshTokenService refreshTokenService,
            RefreshCookieFactory cookieFactory) {
        this.googleLinkService = googleLinkService;
        this.accessTokenIssuer = accessTokenIssuer;
        this.refreshTokenService = refreshTokenService;
        this.cookieFactory = cookieFactory;
    }

    @PostMapping("/api/auth/google/link")
    public ResponseEntity<AuthResponse> link(@Valid @RequestBody GoogleLinkRequest request, HttpServletRequest http) {
        User user = googleLinkService.linkWithPassword(request.token(), request.password());
        IssuedAccessToken access = accessTokenIssuer.issue(user.getId());
        IssuedRefreshToken refresh = refreshTokenService.issueNewFamily(user.getId(), http.getHeader("User-Agent"));
        AuthResponse body = new AuthResponse(access.token(), access.expiresInSeconds(), UserResponse.from(user));
        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        cookieFactory.issue(refresh.rawToken()).toString())
                .body(body);
    }
}
