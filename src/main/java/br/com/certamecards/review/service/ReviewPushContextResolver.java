package br.com.certamecards.review.service;

import br.com.certamecards.card.domain.Card;
import br.com.certamecards.card.service.CardAccessResolver;
import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ReviewPushContextResolver {

    private final CardAccessResolver cardAccessResolver;
    private final Clock clock;

    public ReviewPushContextResolver(CardAccessResolver cardAccessResolver, Clock clock) {
        this.cardAccessResolver = cardAccessResolver;
        this.clock = clock;
    }

    public ReviewPushContext resolve(UUID userId, ReviewPushCommand command) {
        Instant now = clock.instant();
        Set<UUID> cardIds = collectCardIds(command);
        Map<UUID, Card> accessibleCards = cardAccessResolver.resolveAllForRead(userId, cardIds);
        return new ReviewPushContext(userId, accessibleCards, now);
    }

    private Set<UUID> collectCardIds(ReviewPushCommand command) {
        Set<UUID> ids = new HashSet<>();
        command.reviews().forEach(review -> ids.add(review.cardId()));
        command.states().forEach(state -> ids.add(state.cardId()));
        return ids;
    }
}
