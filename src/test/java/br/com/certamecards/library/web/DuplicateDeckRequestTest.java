package br.com.certamecards.library.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class DuplicateDeckRequestTest {

    private final UUID userId = UUID.randomUUID();
    private final UUID sourceId = UUID.randomUUID();

    @Test
    void givenNoFlags_whenConverting_thenCarriesProgressAndKeepsSubscription() {
        var options = new DuplicateDeckRequest(UUID.randomUUID(), null, null)
                .toCommand(userId, sourceId)
                .options();

        assertThat(options.carryProgress()).isTrue();
        assertThat(options.cancelSubscription()).isFalse();
    }

    @Test
    void givenExplicitFlags_whenConverting_thenTheyAreKept() {
        UUID id = UUID.randomUUID();

        var command = new DuplicateDeckRequest(id, false, true).toCommand(userId, sourceId);

        assertThat(command.options().newDeckId()).isEqualTo(id);
        assertThat(command.options().carryProgress()).isFalse();
        assertThat(command.options().cancelSubscription()).isTrue();
        assertThat(command.sourceDeckId()).isEqualTo(sourceId);
    }
}
