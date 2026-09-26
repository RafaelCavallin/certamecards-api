package br.com.certamecards.review.web;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.certamecards.common.sync.EventClock;
import br.com.certamecards.review.service.ReviewLogInput;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.NullNode;

class ReviewLogRequestTest {

    private static final Instant NOW = Instant.parse("2026-09-17T12:00:00Z");
    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    @Test
    void givenNullStateBefore_whenConvertingToInput_thenStateBeforeIsNull() {
        ReviewLogRequest request = new ReviewLogRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "review",
                (short) 3,
                NOW,
                4000,
                null,
                MAPPER.createObjectNode(),
                false,
                UUID.randomUUID(),
                null,
                new EventClock(NOW, 0),
                NOW);

        ReviewLogInput input = request.toInput();

        assertThat(input.stateBefore()).isNull();
    }

    @Test
    void givenJsonNullNodeStateBefore_whenConvertingToInput_thenStateBeforeIsSerializedAsNullLiteral() {
        ReviewLogRequest request = new ReviewLogRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "review",
                (short) 3,
                NOW,
                4000,
                NullNode.getInstance(),
                MAPPER.createObjectNode(),
                false,
                UUID.randomUUID(),
                null,
                new EventClock(NOW, 0),
                NOW);

        ReviewLogInput input = request.toInput();

        assertThat(input.stateBefore()).isEqualTo("null");
    }
}
