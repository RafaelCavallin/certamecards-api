package br.com.certamecards.settings.web;

import br.com.certamecards.settings.domain.UserSettings;
import java.time.LocalDate;

public record UserSettingsResponse(
        int newPerDay,
        int reviewsPerDay,
        int focusMinutes,
        LocalDate examDate,
        String timeZone,
        String theme,
        Long changeSeq) {

    public static UserSettingsResponse from(UserSettings settings) {
        return new UserSettingsResponse(
                settings.getNewPerDay(),
                settings.getReviewsPerDay(),
                settings.getFocusMinutes(),
                settings.getExamDate(),
                settings.getTimeZone(),
                settings.getTheme().code(),
                settings.getChangeSeq());
    }
}
