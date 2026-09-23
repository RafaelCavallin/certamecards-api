package br.com.certamecards.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "certame.sync")
public record SyncProperties(
        int defaultPageLimit, int maxBatchOperations, int maxDependsOn, int conflictRetentionDays) {}
