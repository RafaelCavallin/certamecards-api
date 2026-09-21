package br.com.certamecards.auth.web;

import br.com.certamecards.common.security.ClientOriginProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

@Component
public class GoogleAuthenticationFailureHandler implements AuthenticationFailureHandler {

    private final ClientOriginProperties originProperties;

    public GoogleAuthenticationFailureHandler(ClientOriginProperties originProperties) {
        this.originProperties = originProperties;
    }

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) {
        response.setStatus(302);
        response.setHeader("Location", originProperties.frontendOrigin() + "/entrar?erro=google");
    }
}
