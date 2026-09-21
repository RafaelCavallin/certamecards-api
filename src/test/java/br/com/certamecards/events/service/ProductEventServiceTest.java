package br.com.certamecards.events.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import br.com.certamecards.events.domain.ProductEvent;
import br.com.certamecards.events.domain.ProductEventName;
import br.com.certamecards.events.persistence.ProductEventRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class ProductEventServiceTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-09-17T12:00:00Z");

    private final ProductEventRepository repository = mock(ProductEventRepository.class);
    private final ProductEventService service =
            new ProductEventService(repository, JsonMapper.builder().build());

    @Test
    void givenValidEvent_whenSubmitting_thenSavesWithUserId() {
        UUID userId = UUID.randomUUID();
        SubmitEventCommand command =
                new SubmitEventCommand(UUID.randomUUID(), "session_started", Map.of("scope", "deck"), FIXED_NOW);

        service.submit(userId, List.of(command));

        verify(repository).saveAll(assertSingleEventWith(userId));
    }

    @Test
    void givenNameOutsideClosedList_whenSubmitting_thenDropsSilently() {
        SubmitEventCommand command = new SubmitEventCommand(UUID.randomUUID(), "unknown_event", Map.of(), FIXED_NOW);

        service.submit(null, List.of(command));

        verify(repository).saveAll(List.of());
    }

    @Test
    void givenPropsAboveLimit_whenSubmitting_thenDropsSilently() {
        String oversized = "x".repeat(3000);
        SubmitEventCommand command =
                new SubmitEventCommand(UUID.randomUUID(), "client_error", Map.of("message", oversized), FIXED_NOW);

        service.submit(null, List.of(command));

        verify(repository).saveAll(List.of());
    }

    private List<ProductEvent> assertSingleEventWith(UUID userId) {
        return org.mockito.ArgumentMatchers.argThat(events -> {
            assertThat(events).hasSize(1);
            assertThat(events.get(0).getUserId()).isEqualTo(userId);
            assertThat(events.get(0).getName()).isEqualTo(ProductEventName.SESSION_STARTED);
            return true;
        });
    }
}
