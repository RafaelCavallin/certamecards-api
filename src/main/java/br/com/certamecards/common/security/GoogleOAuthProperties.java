package br.com.certamecards.common.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "certame.google")
public record GoogleOAuthProperties(String clientId, String clientSecret) {}
