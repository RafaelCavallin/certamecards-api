package br.com.certamecards.settings.domain;

import br.com.certamecards.common.sync.SyncMetadata;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "user_settings")
public class UserSettings {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "new_per_day", nullable = false)
    private int newPerDay;

    @Column(name = "reviews_per_day", nullable = false)
    private int reviewsPerDay;

    @Column(name = "focus_minutes", nullable = false)
    private int focusMinutes;

    @Column(name = "exam_date")
    private LocalDate examDate;

    @Column(name = "time_zone", nullable = false)
    private String timeZone;

    @Column(nullable = false)
    private Theme theme;

    @Embedded
    @AttributeOverride(name = "changeSeq", column = @Column(name = "change_seq"))
    private SyncMetadata syncMetadata = new SyncMetadata();

    protected UserSettings() {}

    public UserSettings(UUID userId, String timeZone) {
        this.userId = userId;
        this.timeZone = timeZone;
        this.newPerDay = 20;
        this.reviewsPerDay = 9999;
        this.focusMinutes = 25;
        this.theme = Theme.NOITE;
    }

    public void apply(UpdateSettingsCommand command) {
        this.newPerDay = command.newPerDay();
        this.reviewsPerDay = command.reviewsPerDay();
        this.focusMinutes = command.focusMinutes();
        this.examDate = command.examDate();
        this.timeZone = command.timeZone();
        this.theme = command.theme();
    }

    public UUID getUserId() {
        return userId;
    }

    public int getNewPerDay() {
        return newPerDay;
    }

    public int getReviewsPerDay() {
        return reviewsPerDay;
    }

    public int getFocusMinutes() {
        return focusMinutes;
    }

    public LocalDate getExamDate() {
        return examDate;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public Theme getTheme() {
        return theme;
    }

    public Long getChangeSeq() {
        return syncMetadata.getChangeSeq();
    }
}
