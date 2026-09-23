package br.com.certamecards.officialdeck.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.certamecards.officialdeck.domain.OfficialDeckAdminFilter;
import br.com.certamecards.officialdeck.domain.OfficialDeckAdminSummary;
import br.com.certamecards.officialdeck.domain.OfficialDeckStatus;
import br.com.certamecards.support.PostgresContainerSupport;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(PostgresContainerSupport.class)
class OfficialDeckAdminQueryIT {

    private static final int PAGE_SIZE = 50;

    @Autowired
    private OfficialDeckAdminQuery query;

    private OfficialDeckAdminFilter filter(OfficialDeckStatus status, UUID subjectId, int page, int size) {
        return new OfficialDeckAdminFilter(status, subjectId, page, size);
    }

    @Test
    void givenSeededDrafts_whenFilteringByStatus_thenOnlyThatStatusIsListedAndCounted() {
        List<OfficialDeckAdminSummary> drafts = query.search(filter(OfficialDeckStatus.DRAFT, null, 0, PAGE_SIZE));
        List<OfficialDeckAdminSummary> published =
                query.search(filter(OfficialDeckStatus.PUBLISHED, null, 0, PAGE_SIZE));

        assertThat(drafts).isNotEmpty();
        assertThat(drafts).allMatch(deck -> OfficialDeckStatus.DRAFT.code().equals(deck.status()));
        assertThat(published).isEmpty();
        assertThat(query.count(filter(OfficialDeckStatus.DRAFT, null, 0, PAGE_SIZE)))
                .isEqualTo(drafts.size());
    }

    @Test
    void givenSeededDrafts_whenFilteringBySubject_thenOnlyThatSubjectIsListed() {
        OfficialDeckAdminSummary first =
                query.search(filter(null, null, 0, PAGE_SIZE)).getFirst();

        List<OfficialDeckAdminSummary> bySubject = query.search(filter(null, first.subjectId(), 0, PAGE_SIZE));

        assertThat(bySubject).isNotEmpty().allMatch(deck -> deck.subjectId().equals(first.subjectId()));
        assertThat(query.count(filter(null, first.subjectId(), 0, PAGE_SIZE))).isEqualTo(bySubject.size());
    }

    @Test
    void givenSeededDrafts_whenPagingAndFindingById_thenPageSizeIsRespectedAndDeckIsFound() {
        List<OfficialDeckAdminSummary> firstPage = query.search(filter(null, null, 0, 1));
        List<OfficialDeckAdminSummary> secondPage = query.search(filter(null, null, 1, 1));

        assertThat(firstPage).hasSize(1);
        assertThat(secondPage).hasSize(1);
        assertThat(secondPage.getFirst().id()).isNotEqualTo(firstPage.getFirst().id());
        assertThat(query.findById(firstPage.getFirst().id())).contains(firstPage.getFirst());
        assertThat(query.findById(UUID.randomUUID())).isEmpty();
    }
}
