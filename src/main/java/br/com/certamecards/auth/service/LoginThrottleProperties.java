package br.com.certamecards.auth.service;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "certame.login-throttle")
public record LoginThrottleProperties(int maxFailures, Duration lockWindow) {}
