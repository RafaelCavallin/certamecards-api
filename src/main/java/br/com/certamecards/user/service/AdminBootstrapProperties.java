package br.com.certamecards.user.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "certame.admin")
public record AdminBootstrapProperties(String bootstrapEmail) {}
