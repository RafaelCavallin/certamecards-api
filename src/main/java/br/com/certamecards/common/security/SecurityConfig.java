package br.com.certamecards.common.security;

import br.com.certamecards.common.logging.RequestContextFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class SecurityConfig {

    private static final String[] PUBLIC_POST_ROUTES = {
        "/api/auth/register",
        "/api/auth/confirm-email",
        "/api/auth/resend-confirmation",
        "/api/auth/login",
        "/api/auth/refresh",
        "/api/auth/logout",
        "/api/auth/password/forgot",
        "/api/auth/password/reset",
        "/api/auth/google/link",
        "/api/events"
    };

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            JwtDecoder jwtDecoder,
            UserJwtAuthenticationConverter converter,
            ClientRegistrationRepository clientRegistrationRepository,
            AuthorizationRequestRepository<OAuth2AuthorizationRequest> authorizationRequestRepository,
            AuthenticationSuccessHandler googleSuccessHandler,
            AuthenticationFailureHandler googleFailureHandler,
            ClientOriginProperties clientOriginProperties,
            RegisterRateLimitFilter registerRateLimitFilter,
            EventsRateLimitFilter eventsRateLimitFilter,
            ApiAccessDeniedHandler accessDeniedHandler,
            ObjectMapper objectMapper)
            throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(
                        org.springframework.security.config.http.SessionCreationPolicy.STATELESS))
                .exceptionHandling(handling -> handling.accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(requests -> requests.requestMatchers("/actuator/health/**")
                        .permitAll()
                        .requestMatchers(PUBLIC_POST_ROUTES)
                        .permitAll()
                        .requestMatchers("/api/auth/oauth2/**")
                        .permitAll()
                        .requestMatchers("/api/admin/**")
                        .hasRole("ADMIN")
                        .anyRequest()
                        .authenticated())
                .oauth2ResourceServer(
                        oauth2 -> oauth2.jwt(jwt -> jwt.decoder(jwtDecoder).jwtAuthenticationConverter(converter)))
                .oauth2Login(oauth2 -> oauth2.clientRegistrationRepository(clientRegistrationRepository)
                        .authorizationEndpoint(a -> a.baseUri("/api/auth/oauth2/authorization")
                                .authorizationRequestRepository(authorizationRequestRepository))
                        .redirectionEndpoint(r -> r.baseUri("/api/auth/oauth2/callback/*"))
                        .successHandler(googleSuccessHandler)
                        .failureHandler(googleFailureHandler))
                .addFilterBefore(registerRateLimitFilter, BasicAuthenticationFilter.class)
                .addFilterBefore(eventsRateLimitFilter, BasicAuthenticationFilter.class)
                .addFilterBefore(
                        new OriginValidationFilter(clientOriginProperties, objectMapper),
                        BasicAuthenticationFilter.class)
                .addFilterAfter(new TermsRequiredFilter(objectMapper), BasicAuthenticationFilter.class)
                .addFilterAfter(new RequestContextFilter(), BasicAuthenticationFilter.class);
        return http.build();
    }
}
