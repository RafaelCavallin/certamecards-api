package br.com.certamecards.library.web;

import br.com.certamecards.library.domain.LibraryLimits;
import br.com.certamecards.library.service.ContentCursor;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;

public record ContentParams(UUID after, @Min(1) @Max(LibraryLimits.MAX_CONTENT_PAGE_SIZE) Integer limit) {

    public ContentCursor cursor(int defaultLimit) {
        return new ContentCursor(after, limit == null ? defaultLimit : limit);
    }
}
