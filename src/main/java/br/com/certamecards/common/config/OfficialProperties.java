package br.com.certamecards.common.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "certame.official")
public record OfficialProperties(int contentUpdateBatch, Duration contentUpdateInterval) {}
