package br.com.certamecards.sync.service;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.certamecards.sync.domain.ChangesPage;
import br.com.certamecards.sync.domain.SettingsChange;
import br.com.certamecards.sync.domain.SubjectChange;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ChangesPageBuilderTest {

    @Test
    void givenFewerItemsThanLimit_whenBuilding_thenHasMoreIsFalse() {
        ChangesPageBuilder builder = new ChangesPageBuilder(10);
        builder.addSubjects(List.of(new SubjectChange(UUID.randomUUID(), "Português", true, 5)));

        ChangesPage page = builder.build(0, 10);

        assertThat(page.hasMore()).isFalse();
        assertThat(page.nextCursor()).isEqualTo(5);
        assertThat(builder.itemCount()).isEqualTo(1);
    }

    @Test
    void givenItemsFillingTheLimit_whenBuilding_thenHasMoreIsTrue() {
        ChangesPageBuilder builder = new ChangesPageBuilder(1);
        builder.addSubjects(List.of(new SubjectChange(UUID.randomUUID(), "Português", true, 5)));

        ChangesPage page = builder.build(0, 1);

        assertThat(page.hasMore()).isTrue();
        assertThat(builder.remaining()).isZero();
    }

    @Test
    void givenSettingsNewerThanOtherItems_whenBuilding_thenNextCursorReflectsSettings() {
        ChangesPageBuilder builder = new ChangesPageBuilder(10);
        builder.addSubjects(List.of(new SubjectChange(UUID.randomUUID(), "Português", true, 5)));
        builder.setSettings(
                new SettingsChange(20, 9999, 25, LocalDate.parse("2026-11-08"), "America/Sao_Paulo", "noite", 9));

        ChangesPage page = builder.build(0, 10);

        assertThat(page.nextCursor()).isEqualTo(9);
        assertThat(page.settings().changeSeq()).isEqualTo(9);
    }
}
