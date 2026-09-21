package br.com.certamecards.review.service;

import br.com.certamecards.review.domain.CardState;
import br.com.certamecards.review.domain.CardStateId;
import br.com.certamecards.review.domain.CardStateSnapshot;
import br.com.certamecards.review.domain.FsrsProgressSeed;
import br.com.certamecards.review.domain.ReviewKind;
import br.com.certamecards.review.domain.ReviewLog;
import br.com.certamecards.review.persistence.CardStateRepository;
import br.com.certamecards.review.persistence.ReviewLogRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
public class CardStateService {

    private static final UUID SYSTEM_DEVICE_ID = new UUID(0L, 0L);

    private final CardStateRepository cardStateRepository;
    private final ReviewLogRepository reviewLogRepository;
    private final ObjectMapper objectMapper;

    public CardStateService(
            CardStateRepository cardStateRepository,
            ReviewLogRepository reviewLogRepository,
            ObjectMapper objectMapper) {
        this.cardStateRepository = cardStateRepository;
        this.reviewLogRepository = reviewLogRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public CardState setSuspended(UUID userId, UUID cardId, boolean suspended, Instant now) {
        CardState state = findOrCreate(userId, cardId, now);
        state.setSuspended(suspended, now);
        return cardStateRepository.save(state);
    }

    @Transactional
    public int resetCards(UUID userId, List<UUID> cardIds, Instant now) {
        List<CardState> studiedStates = cardStateRepository.findByIdUserIdAndIdCardIdIn(userId, cardIds).stream()
                .filter(this::hasProgress)
                .toList();
        studiedStates.forEach(state -> resetOne(userId, state, now));
        return studiedStates.size();
    }

    @Transactional
    public CardState seedProgress(UUID userId, UUID cardId, FsrsProgressSeed seed, int reviewCount, Instant now) {
        CardState state = findOrCreate(userId, cardId, now);
        state.seedProgress(seed, reviewCount, now);
        return cardStateRepository.save(state);
    }

    private void resetOne(UUID userId, CardState state, Instant now) {
        String stateBefore = writeSnapshot(CardStateSnapshot.from(state.getProgress()));
        state.reset(now);
        String stateAfter = writeSnapshot(CardStateSnapshot.from(state.getProgress()));
        cardStateRepository.save(state);
        ReviewLog log = new ReviewLog(
                UUID.randomUUID(),
                userId,
                state.getId().getCardId(),
                ReviewKind.RESET,
                now,
                SYSTEM_DEVICE_ID,
                stateAfter);
        log.assignStateBefore(stateBefore);
        log.getSubmission().markReceived(now);
        reviewLogRepository.save(log);
    }

    private boolean hasProgress(CardState state) {
        return state.getProgress().getState().code() != 0;
    }

    private CardState findOrCreate(UUID userId, UUID cardId, Instant now) {
        return cardStateRepository.findByIdUserIdAndIdCardId(userId, cardId).orElseGet(() -> {
            CardState created = new CardState(new CardStateId(userId, cardId), now);
            created.initializeReviewCount(
                    (int) reviewLogRepository.countAcceptedByCardIdAndUserId(cardId, userId), now);
            return created;
        });
    }

    private String writeSnapshot(CardStateSnapshot snapshot) {
        return objectMapper.writeValueAsString(snapshot);
    }
}
