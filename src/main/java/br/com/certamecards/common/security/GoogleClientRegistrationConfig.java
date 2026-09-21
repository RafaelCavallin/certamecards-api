package br.com.certamecards.common.security;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.util.StringUtils;

@Configuration
@EnableConfigurationProperties(GoogleOAuthProperties.class)
public class GoogleClientRegistrationConfig {

    public static final String GOOGLE_REGISTRATION_ID = "google";

    private static final String AUTHORIZATION_URI = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN_URI = "https://www.googleapis.com/oauth2/v4/token";
    private static final String USER_INFO_URI = "https://www.googleapis.com/oauth2/v3/userinfo";
    private static final String JWK_SET_URI = "https://www.googleapis.com/oauth2/v3/certs";
    private static final String ISSUER_URI = "https://accounts.google.com";

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository(GoogleOAuthProperties properties) {
        List<ClientRegistration> registrations = new ArrayList<>();
        if (StringUtils.hasText(properties.clientId()) && StringUtils.hasText(properties.clientSecret())) {
            registrations.add(googleRegistration(properties));
        }
        if (registrations.isEmpty()) {
            return registrationId -> null;
        }
        return new InMemoryClientRegistrationRepository(registrations);
    }

    private ClientRegistration googleRegistration(GoogleOAuthProperties properties) {
        return ClientRegistration.withRegistrationId(GOOGLE_REGISTRATION_ID)
                .clientId(properties.clientId())
                .clientSecret(properties.clientSecret())
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/api/auth/oauth2/callback/{registrationId}")
                .scope("openid", "profile", "email")
                .authorizationUri(AUTHORIZATION_URI)
                .tokenUri(TOKEN_URI)
                .userInfoUri(USER_INFO_URI)
                .userNameAttributeName(IdTokenClaimNames.SUB)
                .jwkSetUri(JWK_SET_URI)
                .issuerUri(ISSUER_URI)
                .clientName("Google")
                .build();
    }
}
