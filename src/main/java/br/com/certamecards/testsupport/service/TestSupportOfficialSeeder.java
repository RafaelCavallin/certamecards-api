package br.com.certamecards.testsupport.service;

import br.com.certamecards.common.config.LibraryProperties;
import br.com.certamecards.deck.domain.Deck;
import br.com.certamecards.officialdeck.domain.OfficialDeckStatus;
import br.com.certamecards.officialdeck.service.CreateOfficialCardCommand;
import br.com.certamecards.officialdeck.service.CreateOfficialDeckCommand;
import br.com.certamecards.officialdeck.service.OfficialCardLifecycleService;
import br.com.certamecards.officialdeck.service.OfficialDeckService;
import br.com.certamecards.officialdeck.service.OfficialDeckStatusService;
import br.com.certamecards.subject.service.SubjectLookup;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class TestSupportOfficialSeeder {

    private static final String DECK_DESCRIPTION = "Deck oficial criado pelo cenário de teste.";

    private final OfficialDeckService deckService;
    private final OfficialCardLifecycleService cardService;
    private final OfficialDeckStatusService statusService;
    private final SubjectLookup subjectLookup;
    private final TestSupportSubscriberSeeder subscriberSeeder;
    private final LibraryProperties properties;

    public TestSupportOfficialSeeder(
            OfficialDeckService deckService,
            OfficialCardLifecycleService cardService,
            OfficialDeckStatusService statusService,
            SubjectLookup subjectLookup,
            TestSupportSubscriberSeeder subscriberSeeder,
            LibraryProperties properties) {
        this.deckService = deckService;
        this.cardService = cardService;
        this.statusService = statusService;
        this.subjectLookup = subjectLookup;
        this.subscriberSeeder = subscriberSeeder;
        this.properties = properties;
    }

    public SeedResult seed(SeedCommand command) {
        UUID deckId = createDraft(command);
        List<UUID> cardIds = createCards(command, deckId);
        Deck draft = deckService.findOfficial(deckId);
        statusService.changeStatus(command.ownerId(), deckId, OfficialDeckStatus.PUBLISHED, draft.getVersion());
        if (command.scenario().withSubscribers()) {
            subscriberSeeder.seed(deckId);
        }
        return new SeedResult(deckId, cardIds);
    }

    private UUID createDraft(SeedCommand command) {
        UUID subjectId =
                subjectLookup.resolveActiveByNameOrFirst(command.subjectName()).getId();
        UUID deckId = UUID.randomUUID();
        String name = "Oficial " + command.scenario().code() + " " + deckId;
        deckService.create(command.ownerId(), new CreateOfficialDeckCommand(deckId, subjectId, name, DECK_DESCRIPTION));
        return deckId;
    }

    private List<UUID> createCards(SeedCommand command, UUID deckId) {
        List<UUID> cardIds = new ArrayList<>();
        int total = Math.max(command.count(), properties.minCardsToPublish());
        for (int index = 0; index < total; index++) {
            UUID cardId = UUID.randomUUID();
            cardService.create(
                    command.ownerId(),
                    new CreateOfficialCardCommand(cardId, deckId, "Frente " + index, "Verso " + index, null));
            cardIds.add(cardId);
        }
        return cardIds;
    }
}
