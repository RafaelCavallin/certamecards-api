package br.com.certamecards.library.persistence;

import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.common.error.ErrorCode;
import br.com.certamecards.deck.domain.Deck;
import br.com.certamecards.deck.persistence.DeckRepository;
import br.com.certamecards.library.domain.DeckCopyRequest;
import br.com.certamecards.library.domain.DuplicationCounts;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class DeckCopyWriter {

    private final JdbcClient jdbcClient;
    private final DeckRepository deckRepository;

    public DeckCopyWriter(JdbcClient jdbcClient, DeckRepository deckRepository) {
        this.jdbcClient = jdbcClient;
        this.deckRepository = deckRepository;
    }

    public Optional<Deck> findExisting(UUID userId, UUID deckId) {
        Optional<Deck> found = deckRepository.findById(deckId);
        if (found.isPresent() && !userId.equals(found.get().getOwnerId())) {
            throw ApiException.of(ErrorCode.NOT_FOUND);
        }
        return found;
    }

    public Deck load(UUID deckId) {
        return deckRepository.findById(deckId).orElseThrow(() -> ApiException.of(ErrorCode.NOT_FOUND));
    }

    public DuplicationCounts copy(DeckCopyRequest request, Instant now) {
        Long deckChangeSeq = insertDeck(request, now);
        return jdbcClient
                .sql(DeckCopySql.COPY_CARDS)
                .param("userId", request.userId())
                .param("sourceId", request.source().getId())
                .param("newDeckId", request.newDeckId())
                .param("carry", request.carryStates())
                .param("now", Timestamp.from(now))
                .query((rs, rowNum) -> new DuplicationCounts(
                        rs.getInt("copied_cards"), rs.getInt("carried_states"), deckChangeSeq - 1))
                .single();
    }

    public DuplicationCounts describe(UUID userId, Deck deck) {
        return jdbcClient
                .sql(DeckCopySql.DESCRIBE)
                .param("userId", userId)
                .param("deckId", deck.getId())
                .query((rs, rowNum) -> new DuplicationCounts(
                        rs.getInt("copied_cards"), rs.getInt("carried_states"), deck.getChangeSeq() - 1))
                .single();
    }

    private Long insertDeck(DeckCopyRequest request, Instant now) {
        Deck source = request.source();
        return jdbcClient
                .sql(DeckCopySql.INSERT_DECK)
                .param("newDeckId", request.newDeckId())
                .param("userId", request.userId())
                .param("subjectId", source.getSubjectId())
                .param("name", source.getName())
                .param("description", source.getDescription())
                .param("sourceId", source.getId())
                .param("now", Timestamp.from(now))
                .query(Long.class)
                .single();
    }
}
