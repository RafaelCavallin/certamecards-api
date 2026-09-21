package br.com.certamecards.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "certame.library")
public record LibraryProperties(
        int progressRetentionDays,
        int previewCards,
        int minCardsToPublish,
        int pageSize,
        int contentPageSize,
        int suggestions) {}
