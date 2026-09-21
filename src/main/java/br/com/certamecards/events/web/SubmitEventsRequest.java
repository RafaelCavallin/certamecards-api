package br.com.certamecards.events.web;

import br.com.certamecards.events.domain.ProductEventLimits;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record SubmitEventsRequest(
        @NotNull @Valid @Size(max = ProductEventLimits.MAX_EVENTS_PER_REQUEST) List<ProductEventRequest> events) {}
