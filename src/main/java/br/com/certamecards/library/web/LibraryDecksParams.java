package br.com.certamecards.library.web;

import br.com.certamecards.library.domain.LibraryLimits;
import br.com.certamecards.library.domain.LibraryPageRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record LibraryDecksParams(
        @Size(max = LibraryLimits.MAX_QUERY_LENGTH) String q,
        UUID subjectId,
        @Min(0) Integer page,
        @Min(1) @Max(LibraryLimits.MAX_PAGE_SIZE) Integer size) {

    public LibraryPageRequest pageRequest(int defaultSize) {
        return new LibraryPageRequest(page == null ? 0 : page, size == null ? defaultSize : size);
    }
}
