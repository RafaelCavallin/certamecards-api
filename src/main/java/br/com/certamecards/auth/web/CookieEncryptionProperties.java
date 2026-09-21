package br.com.certamecards.auth.web;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "certame.cookie-encryption")
public record CookieEncryptionProperties(String key) {}
