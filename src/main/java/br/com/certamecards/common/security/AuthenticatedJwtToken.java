package br.com.certamecards.common.security;

import java.util.Collection;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

public class AuthenticatedJwtToken extends AbstractAuthenticationToken {

    private final Jwt jwt;
    private final AuthenticatedUser principal;

    public AuthenticatedJwtToken(Jwt jwt, AuthenticatedUser principal, Collection<GrantedAuthority> authorities) {
        super(authorities);
        this.jwt = jwt;
        this.principal = principal;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return jwt;
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }
}
