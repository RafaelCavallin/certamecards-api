package br.com.certamecards.library.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class LibraryParamsTest {

    @Test
    void givenNoPageOrSize_whenBuildingPageRequest_thenUsesDefaults() {
        var request = new LibraryDecksParams(null, null, null, null).pageRequest(20);

        assertThat(request.page()).isZero();
        assertThat(request.size()).isEqualTo(20);
        assertThat(request.offset()).isZero();
    }

    @Test
    void givenPageAndSize_whenBuildingPageRequest_thenOffsetIsPageTimesSize() {
        var request = new LibraryDecksParams("q", null, 2, 10).pageRequest(20);

        assertThat(request.offset()).isEqualTo(20);
    }

    @Test
    void givenNoLimit_whenBuildingCursor_thenUsesDefaultLimit() {
        UUID after = UUID.randomUUID();

        var cursor = new ContentParams(after, null).cursor(500);

        assertThat(cursor.after()).isEqualTo(after);
        assertThat(cursor.limit()).isEqualTo(500);
    }

    @Test
    void givenLimit_whenBuildingCursor_thenUsesIt() {
        assertThat(new ContentParams(null, 50).cursor(500).limit()).isEqualTo(50);
    }
}
