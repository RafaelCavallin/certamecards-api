package br.com.certamecards.sync.service;

import java.time.LocalDate;

public record SettingsPatchPayload(
        Integer newPerDay,
        Integer reviewsPerDay,
        Integer focusMinutes,
        LocalDate examDate,
        String timeZone,
        String theme) {}
