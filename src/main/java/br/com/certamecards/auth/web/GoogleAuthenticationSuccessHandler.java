package br.com.certamecards.auth.web;

import br.com.certamecards.auth.service.AccessTokenIssuer;
import br.com.certamecards.auth.service.GoogleLinkService;
import br.com.certamecards.auth.service.GoogleLoginHandler;
import br.com.certamecards.auth.service.GoogleLoginOutcome;
import br.com.certamecards.auth.service.GoogleProfile;
import br.com.certamecards.auth.service.IssuedRefreshToken;
import br.com.certamecards.auth.service.RefreshTokenService;
import br.com.certamecards.common.security.ClientOriginProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class GoogleAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final GoogleLoginHandler loginHandler;
    private final GoogleLinkService linkService;
    private final AccessTokenIssuer accessTokenIssuer;
    private final RefreshTokenService refreshTokenService;
    private final RefreshCookieFactory cookieFactory;
    private final CookieAuthorizationRequestRepository authorizationRequestRepository;
    private final ClientOriginProperties originProperties;

    public GoogleAuthenticationSuccessHandler(
            GoogleLoginHandler loginHandler,
            GoogleLinkService linkService,
            AccessTokenIssuer accessTokenIssuer,
            RefreshTokenService refreshTokenService,
            RefreshCookieFactory cookieFactory,
            CookieAuthorizationRequestRepository authorizationRequestRepository,
            ClientOriginProperties originProperties) {
        this.loginHandler = loginHandler;
        this.linkService = linkService;
        this.accessTokenIssuer = accessTokenIssuer;
        this.refreshTokenService = refreshTokenService;
        this.cookieFactory = cookieFactory;
        this.authorizationRequestRepository = authorizationRequestRepository;
        this.originProperties = originProperties;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        OidcUser oidcUser = (OidcUser) authentication.getPrincipal();
        if ("reauth".equals(authorizationRequestRepository.readIntent(request))) {
            redirectReauth(response, oidcUser.getSubject());
            return;
        }
        GoogleProfile profile = new GoogleProfile(
                oidcUser.getEmail(),
                oidcUser.getSubject(),
                Boolean.TRUE.equals(oidcUser.getEmailVerified()),
                oidcUser.getFullName());
        handleLoginOutcome(response, loginHandler.handleLogin(profile));
    }

    private void handleLoginOutcome(HttpServletResponse response, GoogleLoginOutcome outcome) {
        if (outcome instanceof GoogleLoginOutcome.LinkRequired linkRequired) {
            redirect(response, "/entrar/vincular#token=" + linkRequired.rawToken());
            return;
        }
        GoogleLoginOutcome.Concluded concluded = (GoogleLoginOutcome.Concluded) outcome;
        IssuedRefreshToken refresh = refreshTokenService.issueNewFamily(concluded.userId(), null);
        response.addHeader(
                HttpHeaders.SET_COOKIE, cookieFactory.issue(refresh.rawToken()).toString());
        redirect(response, "/entrar/concluir");
    }

    private void redirectReauth(HttpServletResponse response, String subject) {
        String token = linkService.handleReauth(subject);
        redirect(response, "/ajustes/excluir-conta#reauth=" + token);
    }

    private void redirect(HttpServletResponse response, String path) {
        response.setStatus(302);
        response.setHeader("Location", originProperties.frontendOrigin() + path);
    }
}
