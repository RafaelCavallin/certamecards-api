package br.com.certamecards.settings.domain;

import java.time.LocalDate;

public record UpdateSettingsCommand(
        int newPerDay, int reviewsPerDay, int focusMinutes, LocalDate examDate, String timeZone, Theme theme) {}
