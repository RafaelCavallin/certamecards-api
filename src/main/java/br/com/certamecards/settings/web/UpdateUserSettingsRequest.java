package br.com.certamecards.settings.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record UpdateUserSettingsRequest(
        @Min(0) @Max(500) int newPerDay,
        @Min(0) @Max(9999) int reviewsPerDay,
        @Min(15) @Max(60) int focusMinutes,
        LocalDate examDate,
        @NotBlank String timeZone,
        @NotBlank String theme) {}
