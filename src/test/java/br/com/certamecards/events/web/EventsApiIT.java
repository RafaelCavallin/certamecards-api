package br.com.certamecards.events.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.certamecards.events.persistence.ProductEventRepository;
import br.com.certamecards.support.PostgresContainerSupport;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PostgresContainerSupport.class)
class EventsApiIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductEventRepository productEventRepository;

    @Test
    void givenValidEvent_whenSubmittingWithoutAuth_thenAcceptedAndPersisted() throws Exception {
        UUID eventId = UUID.randomUUID();

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventsBody(eventId, "session_started")))
                .andExpect(status().isAccepted());

        assertThat(productEventRepository.findById(eventId)).isPresent();
    }

    @Test
    void givenLibraryEvent_whenSubmitting_thenAcceptedAndPersisted() throws Exception {
        UUID eventId = UUID.randomUUID();

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventsBody(eventId, "deck_subscribed")))
                .andExpect(status().isAccepted());

        assertThat(productEventRepository.findById(eventId)).isPresent();
    }

    @Test
    void givenNameOutsideClosedList_whenSubmitting_thenAcceptedButNotPersisted() throws Exception {
        UUID eventId = UUID.randomUUID();

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventsBody(eventId, "unknown_event")))
                .andExpect(status().isAccepted());

        assertThat(productEventRepository.findById(eventId)).isEmpty();
    }

    private String eventsBody(UUID id, String name) {
        return "{\"events\":[{\"id\":\"" + id + "\",\"name\":\"" + name
                + "\",\"props\":{},\"occurredAt\":\"2026-09-17T12:00:00Z\"}]}";
    }
}
