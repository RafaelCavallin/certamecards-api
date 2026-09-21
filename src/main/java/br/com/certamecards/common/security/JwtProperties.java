package br.com.certamecards.common.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "certame.jwt")
public record JwtProperties(String privateKey, String publicKey, Duration accessTokenTtl) {}
