package br.com.certamecards.sync.service;

import br.com.certamecards.sync.domain.EventClock;
import br.com.certamecards.sync.domain.EventOrder;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class EventOrderNormalizer {

    public EventOrder normalize(
            EventClock clock, UUID deviceId, UUID operationId, Instant observedServerTime, Instant receivedAt) {
        Instant lowerBound = lowerBound(observedServerTime, receivedAt);
        Instant eventAt = clamp(clock.wallTime(), lowerBound, receivedAt);
        return new EventOrder(eventAt, clock.logicalCounter(), deviceId, operationId);
    }

    private Instant lowerBound(Instant observedServerTime, Instant receivedAt) {
        if (observedServerTime == null || observedServerTime.isAfter(receivedAt)) {
            return receivedAt;
        }
        return observedServerTime;
    }

    private Instant clamp(Instant value, Instant lowerBound, Instant upperBound) {
        if (value.isBefore(lowerBound)) {
            return lowerBound;
        }
        return value.isAfter(upperBound) ? upperBound : value;
    }
}
