package br.com.certamecards.common.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "certame.register-rate-limit")
public record RegisterRateLimitProperties(int maxRequests, Duration window) {}
