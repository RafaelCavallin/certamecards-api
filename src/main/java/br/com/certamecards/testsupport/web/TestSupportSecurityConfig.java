package br.com.certamecards.testsupport.web;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

@Configuration
@ConditionalOnProperty(prefix = "certame.test-support", name = "enabled", havingValue = "true")
public class TestSupportSecurityConfig {

    static final String USERS_PATH = "/api/test-support/users";

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityFilterChain testSupportUsersFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher(PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, USERS_PATH))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> requests.anyRequest().permitAll());
        return http.build();
    }
}
