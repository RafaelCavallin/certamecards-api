package br.com.certamecards.sync.domain;

import java.time.LocalDate;

public record SettingsChange(
        int newPerDay,
        int reviewsPerDay,
        int focusMinutes,
        LocalDate examDate,
        String timeZone,
        String theme,
        long changeSeq) {}
