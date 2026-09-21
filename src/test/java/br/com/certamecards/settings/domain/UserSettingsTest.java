package br.com.certamecards.settings.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class UserSettingsTest {

    @Test
    void givenUserIdAndTimeZone_whenConstructing_thenDefaultsAreApplied() {
        UUID userId = UUID.randomUUID();
        UserSettings settings = new UserSettings(userId, "America/Sao_Paulo");
        assertThat(settings.getUserId()).isEqualTo(userId);
        assertThat(settings.getTimeZone()).isEqualTo("America/Sao_Paulo");
        assertThat(settings.getNewPerDay()).isEqualTo(20);
        assertThat(settings.getReviewsPerDay()).isEqualTo(9999);
        assertThat(settings.getFocusMinutes()).isEqualTo(25);
        assertThat(settings.getTheme()).isEqualTo(Theme.NOITE);
        assertThat(settings.getExamDate()).isNull();
        assertThat(settings.getChangeSeq()).isNull();
    }
}
