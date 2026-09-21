package br.com.certamecards.user.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "certame.terms")
public record TermsProperties(String currentVersion) {}
