package br.com.certamecards.library.service;

import br.com.certamecards.deck.domain.Deck;
import br.com.certamecards.library.domain.DeckCopyRequest;
import br.com.certamecards.library.domain.DuplicateDeckCommand;
import br.com.certamecards.library.domain.DuplicationCounts;
import br.com.certamecards.library.domain.DuplicationPlan;
import br.com.certamecards.library.domain.DuplicationSource;
import br.com.certamecards.library.persistence.DeckCopyWriter;
import br.com.certamecards.library.persistence.UserCardCountsQuery;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeckDuplicationService {

    private final DeckCopyWriter copyWriter;
    private final DuplicationSourceLoader sourceLoader;
    private final DuplicationPlanner planner;
    private final UserCardCountsQuery cardCountsQuery;
    private final SubscriptionService subscriptionService;
    private final Clock clock;

    public DeckDuplicationService(
            DeckCopyWriter copyWriter,
            DuplicationSourceLoader sourceLoader,
            DuplicationPlanner planner,
            UserCardCountsQuery cardCountsQuery,
            SubscriptionService subscriptionService,
            Clock clock) {
        this.copyWriter = copyWriter;
        this.sourceLoader = sourceLoader;
        this.planner = planner;
        this.cardCountsQuery = cardCountsQuery;
        this.subscriptionService = subscriptionService;
        this.clock = clock;
    }

    @Transactional
    public DuplicationResult duplicate(DuplicateDeckCommand command) {
        UUID userId = command.userId();
        Optional<Deck> existing =
                copyWriter.findExisting(userId, command.options().newDeckId());
        if (existing.isPresent()) {
            return new DuplicationResult(existing.get(), copyWriter.describe(userId, existing.get()), false);
        }
        DuplicationSource source = sourceLoader.load(userId, command.sourceDeckId());
        DuplicationPlan plan = planner.plan(command, source, cardCountsQuery.usedCards(userId));
        DeckCopyRequest request =
                new DeckCopyRequest(userId, source.deck(), command.options().newDeckId(), plan.carryStates());
        DuplicationCounts counts = copyWriter.copy(request, clock.instant());
        if (plan.cancelSubscription()) {
            subscriptionService.cancel(userId, command.sourceDeckId());
        }
        return new DuplicationResult(copyWriter.load(request.newDeckId()), counts, true);
    }
}
