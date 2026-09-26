package br.com.certamecards.common.sync;

import java.time.Instant;
import java.util.Comparator;
import java.util.UUID;

public record EventOrder(Instant eventAt, int logicalCounter, UUID deviceId, UUID operationId) {

    public static final Comparator<EventOrder> CANONICAL = Comparator.comparing(EventOrder::eventAt)
            .thenComparingInt(EventOrder::logicalCounter)
            .thenComparing(EventOrder::deviceId, EventOrder::compareUuidBinary)
            .thenComparing(EventOrder::operationId, EventOrder::compareUuidBinary);

    public static int compareUuidBinary(UUID left, UUID right) {
        int mostSignificant = Long.compareUnsigned(left.getMostSignificantBits(), right.getMostSignificantBits());
        if (mostSignificant != 0) {
            return mostSignificant;
        }
        return Long.compareUnsigned(left.getLeastSignificantBits(), right.getLeastSignificantBits());
    }
}
