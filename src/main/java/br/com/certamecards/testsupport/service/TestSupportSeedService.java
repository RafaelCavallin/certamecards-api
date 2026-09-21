package br.com.certamecards.testsupport.service;

import br.com.certamecards.card.service.CardContent;
import br.com.certamecards.card.service.CardService;
import br.com.certamecards.card.service.CreateCardCommand;
import br.com.certamecards.deck.service.CreateDeckCommand;
import br.com.certamecards.deck.service.DeckContent;
import br.com.certamecards.deck.service.DeckCreationResult;
import br.com.certamecards.deck.service.DeckService;
import br.com.certamecards.review.domain.CardLearningState;
import br.com.certamecards.review.domain.FsrsProgressSeed;
import br.com.certamecards.review.service.CardStateService;
import br.com.certamecards.subject.domain.Subject;
import br.com.certamecards.subject.service.SubjectLookup;
import br.com.certamecards.testsupport.domain.SeedScenario;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TestSupportSeedService {

    private final DeckService deckService;
    private final CardService cardService;
    private final CardStateService cardStateService;
    private final SubjectLookup subjectLookup;
    private final Clock clock;

    public TestSupportSeedService(
            DeckService deckService,
            CardService cardService,
            CardStateService cardStateService,
            SubjectLookup subjectLookup,
            Clock clock) {
        this.deckService = deckService;
        this.cardService = cardService;
        this.cardStateService = cardStateService;
        this.subjectLookup = subjectLookup;
        this.clock = clock;
    }

    @Transactional
    public SeedResult seed(SeedCommand command) {
        Subject subject = subjectLookup.resolveActiveByNameOrFirst(command.subjectName());
        DeckCreationResult deck = createDeck(command, subject.getId());
        List<UUID> cardIds = new ArrayList<>();
        for (int index = 0; index < command.count(); index++) {
            UUID cardId = createCard(command, deck.deck().getId(), index);
            applyScenario(command, cardId);
            cardIds.add(cardId);
        }
        return new SeedResult(deck.deck().getId(), cardIds);
    }

    private DeckCreationResult createDeck(SeedCommand command, UUID subjectId) {
        DeckContent content = new DeckContent("Seed " + command.scenario().code(), null);
        return deckService.create(new CreateDeckCommand(UUID.randomUUID(), command.ownerId(), subjectId, content));
    }

    private UUID createCard(SeedCommand command, UUID deckId, int index) {
        UUID cardId = UUID.randomUUID();
        CardContent content = new CardContent("Frente " + index, "Verso " + index, null);
        cardService.create(new CreateCardCommand(cardId, deckId, command.ownerId(), content));
        return cardId;
    }

    private void applyScenario(SeedCommand command, UUID cardId) {
        Instant now = clock.instant();
        if (command.scenario() == SeedScenario.DUE_CARDS) {
            FsrsProgressSeed seed = new FsrsProgressSeed(
                    CardLearningState.REVIEW,
                    4.2,
                    5.1,
                    now.minus(Duration.ofMinutes(1)),
                    now.minus(Duration.ofDays(1)),
                    1,
                    0,
                    0,
                    1);
            cardStateService.seedProgress(command.ownerId(), cardId, seed, 1, now);
        }
        if (command.scenario() == SeedScenario.LEECH_CARD) {
            FsrsProgressSeed seed = new FsrsProgressSeed(
                    CardLearningState.REVIEW, 2.1, 8.9, now, now.minus(Duration.ofDays(1)), 8, 8, 0, 1);
            cardStateService.seedProgress(command.ownerId(), cardId, seed, 8, now);
        }
    }
}
