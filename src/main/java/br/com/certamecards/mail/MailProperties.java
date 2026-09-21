package br.com.certamecards.mail;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "certame.mail")
public record MailProperties(String fromAddress, String fromName) {}
