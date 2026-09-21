package br.com.certamecards.events.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProductEventTest {

    @Test
    void givenEventFields_whenConstructing_thenGettersReturnValues() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant occurredAt = Instant.parse("2026-09-17T12:35:00Z");
        ProductEvent event = new ProductEvent(id, userId, ProductEventName.SESSION_ENDED, "{}", occurredAt);
        assertThat(event.getId()).isEqualTo(id);
        assertThat(event.getUserId()).isEqualTo(userId);
        assertThat(event.getName()).isEqualTo(ProductEventName.SESSION_ENDED);
        assertThat(event.getProps()).isEqualTo("{}");
        assertThat(event.getOccurredAt()).isEqualTo(occurredAt);
    }
}
