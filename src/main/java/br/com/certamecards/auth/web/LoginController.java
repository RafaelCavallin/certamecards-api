package br.com.certamecards.auth.web;

import br.com.certamecards.auth.service.LoginCommand;
import br.com.certamecards.auth.service.LoginResult;
import br.com.certamecards.auth.service.LoginService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LoginController {

    private final LoginService loginService;
    private final RefreshCookieFactory cookieFactory;

    public LoginController(LoginService loginService, RefreshCookieFactory cookieFactory) {
        this.loginService = loginService;
        this.cookieFactory = cookieFactory;
    }

    @PostMapping("/api/auth/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        LoginResult result = loginService.login(
                new LoginCommand(request.email(), request.password()),
                http.getRemoteAddr(),
                http.getHeader("User-Agent"));
        AuthResponse body = new AuthResponse(
                result.accessToken().token(),
                result.accessToken().expiresInSeconds(),
                UserResponse.from(result.user()));
        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        cookieFactory.issue(result.refreshToken().rawToken()).toString())
                .body(body);
    }
}
