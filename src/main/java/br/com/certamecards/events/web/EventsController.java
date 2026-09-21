package br.com.certamecards.events.web;

import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.common.error.ErrorCode;
import br.com.certamecards.common.security.AuthenticatedUser;
import br.com.certamecards.events.domain.ProductEventLimits;
import br.com.certamecards.events.service.ProductEventService;
import br.com.certamecards.events.service.SubmitEventCommand;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EventsController {

    private final ProductEventService eventService;
    private final MeterRegistry meterRegistry;

    public EventsController(ProductEventService eventService, MeterRegistry meterRegistry) {
        this.eventService = eventService;
        this.meterRegistry = meterRegistry;
    }

    @PostMapping("/api/events")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void submit(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody SubmitEventsRequest request,
            HttpServletRequest servletRequest) {
        ensureWithinSizeLimit(servletRequest);
        UUID userId = principal == null ? null : principal.id();
        eventService.submit(userId, toCommands(request));
        meterRegistry.counter("events.received").increment(request.events().size());
    }

    private void ensureWithinSizeLimit(HttpServletRequest servletRequest) {
        long contentLength = servletRequest.getContentLengthLong();
        if (contentLength > ProductEventLimits.MAX_REQUEST_BYTES) {
            throw ApiException.of(ErrorCode.VALIDATION_FAILED);
        }
    }

    private List<SubmitEventCommand> toCommands(SubmitEventsRequest request) {
        return request.events().stream()
                .map(event -> new SubmitEventCommand(event.id(), event.name(), event.props(), event.occurredAt()))
                .toList();
    }
}
