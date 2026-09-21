package br.com.certamecards.auth.service;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "certame.refresh-cookie")
public record RefreshCookieProperties(String name, Duration ttl, boolean secure) {}
