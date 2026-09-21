package br.com.certamecards.common.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "certame.client")
public record ClientOriginProperties(String frontendOrigin, String clientHeaderValue) {}
