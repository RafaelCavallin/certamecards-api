package br.com.certamecards.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.certamecards.review.domain.CardLearningState;
import br.com.certamecards.review.domain.CardState;
import br.com.certamecards.review.domain.CardStateId;
import br.com.certamecards.review.domain.FsrsProgressSeed;
import br.com.certamecards.review.persistence.CardStateRepository;
import br.com.certamecards.review.persistence.ReviewLogRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class CardStateServiceTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-09-17T12:00:00Z");

    private final CardStateRepository cardStateRepository = mock(CardStateRepository.class);
    private final ReviewLogRepository reviewLogRepository = mock(ReviewLogRepository.class);
    private final CardStateService service = new CardStateService(
            cardStateRepository, reviewLogRepository, JsonMapper.builder().build());

    @Test
    void givenNoExistingState_whenSuspending_thenCreatesStateWithAcceptedReviewCount() {
        UUID userId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();
        when(cardStateRepository.findByIdUserIdAndIdCardId(userId, cardId)).thenReturn(Optional.empty());
        when(reviewLogRepository.countAcceptedByCardIdAndUserId(cardId, userId)).thenReturn(3L);
        when(cardStateRepository.save(any())).thenAnswer(call -> call.getArgument(0));

        CardState state = service.setSuspended(userId, cardId, true, FIXED_NOW);

        assertThat(state.isSuspended()).isTrue();
        assertThat(state.getReviewCount()).isEqualTo(3);
    }

    @Test
    void givenExistingState_whenSuspending_thenDoesNotTouchFsrsFields() {
        UUID userId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();
        CardState existing = new CardState(new CardStateId(userId, cardId), FIXED_NOW);
        existing.seedProgress(
                new FsrsProgressSeed(CardLearningState.REVIEW, 4.2, 5.1, FIXED_NOW, FIXED_NOW, 3, 0, 0, 14),
                3,
                FIXED_NOW);
        when(cardStateRepository.findByIdUserIdAndIdCardId(userId, cardId)).thenReturn(Optional.of(existing));
        when(cardStateRepository.save(any())).thenAnswer(call -> call.getArgument(0));

        CardState result = service.setSuspended(userId, cardId, true, FIXED_NOW.plusSeconds(60));

        assertThat(result.isSuspended()).isTrue();
        assertThat(result.getProgress().getState()).isEqualTo(CardLearningState.REVIEW);
        assertThat(result.getReviewCount()).isEqualTo(3);
    }

    @Test
    void givenStudiedCard_whenResetting_thenInsertsResetLogAndReturnsCount() {
        UUID userId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();
        CardState studied = new CardState(new CardStateId(userId, cardId), FIXED_NOW);
        studied.seedProgress(
                new FsrsProgressSeed(CardLearningState.REVIEW, 4.2, 5.1, FIXED_NOW, FIXED_NOW, 3, 0, 0, 14),
                3,
                FIXED_NOW);
        when(cardStateRepository.findByIdUserIdAndIdCardIdIn(userId, List.of(cardId)))
                .thenReturn(List.of(studied));
        when(cardStateRepository.save(any())).thenAnswer(call -> call.getArgument(0));

        int resetCount = service.resetCards(userId, List.of(cardId), FIXED_NOW.plusSeconds(60));

        assertThat(resetCount).isEqualTo(1);
        assertThat(studied.getProgress().getState()).isEqualTo(CardLearningState.NEW);
        assertThat(studied.getReviewCount()).isEqualTo(4);
        verify(reviewLogRepository).save(any());
    }

    @Test
    void givenNewCard_whenResetting_thenIsSkipped() {
        UUID userId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();
        CardState fresh = new CardState(new CardStateId(userId, cardId), FIXED_NOW);
        when(cardStateRepository.findByIdUserIdAndIdCardIdIn(userId, List.of(cardId)))
                .thenReturn(List.of(fresh));

        int resetCount = service.resetCards(userId, List.of(cardId), FIXED_NOW.plusSeconds(60));

        assertThat(resetCount).isZero();
    }
}
