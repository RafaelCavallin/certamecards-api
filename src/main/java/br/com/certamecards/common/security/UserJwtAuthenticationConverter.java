package br.com.certamecards.common.security;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.stereotype.Component;

@Component
public class UserJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UserAuthCache userAuthCache;

    public UserJwtAuthenticationConverter(UserAuthCache userAuthCache) {
        this.userAuthCache = userAuthCache;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        AuthSnapshot snapshot = resolveSnapshot(userId);
        AuthenticatedUser principal = new AuthenticatedUser(userId, snapshot.role(), snapshot.termsAccepted());
        List<GrantedAuthority> authorities =
                List.of(new SimpleGrantedAuthority("ROLE_" + snapshot.role().name()));
        return new AuthenticatedJwtToken(jwt, principal, authorities);
    }

    private AuthSnapshot resolveSnapshot(UUID userId) {
        Optional<AuthSnapshot> snapshot = userAuthCache.snapshotOf(userId);
        if (snapshot.isEmpty()) {
            throw new InvalidBearerTokenException("unauthenticated");
        }
        return snapshot.get();
    }
}
