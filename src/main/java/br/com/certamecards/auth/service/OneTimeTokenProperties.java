package br.com.certamecards.auth.service;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "certame.one-time-token")
public record OneTimeTokenProperties(
        Duration confirmEmailTtl, Duration resetPasswordTtl, Duration linkGoogleTtl, Duration reauthTtl) {}
