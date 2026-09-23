package br.com.certamecards.sync.service;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.certamecards.sync.domain.EventClock;
import br.com.certamecards.sync.domain.EventOrder;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EventOrderNormalizerTest {

    private static final Instant RECEIVED_AT = Instant.parse("2026-09-21T14:00:00Z");
    private static final UUID DEVICE_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID OPERATION_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    private final EventOrderNormalizer normalizer = new EventOrderNormalizer();

    @Test
    void givenWallTimeWithinBounds_whenNormalizing_thenKeepsWallTime() {
        Instant wallTime = RECEIVED_AT.minusSeconds(5);
        EventOrder order = normalizer.normalize(
                new EventClock(wallTime, 0), DEVICE_ID, OPERATION_ID, RECEIVED_AT.minusSeconds(10), RECEIVED_AT);

        assertThat(order.eventAt()).isEqualTo(wallTime);
    }

    @Test
    void givenWallTimeBeforeObservedServerTime_whenNormalizing_thenClampsToLowerBound() {
        Instant observedServerTime = RECEIVED_AT.minusSeconds(10);
        Instant wallTime = RECEIVED_AT.minusSeconds(3600);
        EventOrder order = normalizer.normalize(
                new EventClock(wallTime, 0), DEVICE_ID, OPERATION_ID, observedServerTime, RECEIVED_AT);

        assertThat(order.eventAt()).isEqualTo(observedServerTime);
    }

    @Test
    void givenWallTimeAfterReceivedAt_whenNormalizing_thenClampsToReceivedAt() {
        Instant wallTime = RECEIVED_AT.plusSeconds(3600);
        EventOrder order = normalizer.normalize(
                new EventClock(wallTime, 0), DEVICE_ID, OPERATION_ID, RECEIVED_AT.minusSeconds(10), RECEIVED_AT);

        assertThat(order.eventAt()).isEqualTo(RECEIVED_AT);
    }

    @Test
    void givenObservedServerTimeNull_whenNormalizing_thenUsesReceivedAtAsLowerBound() {
        Instant wallTime = RECEIVED_AT.minusSeconds(3600);
        EventOrder order =
                normalizer.normalize(new EventClock(wallTime, 0), DEVICE_ID, OPERATION_ID, null, RECEIVED_AT);

        assertThat(order.eventAt()).isEqualTo(RECEIVED_AT);
    }

    @Test
    void givenObservedServerTimeAfterReceivedAt_whenNormalizing_thenTreatedAsInvalidAndUsesReceivedAt() {
        Instant invalidObserved = RECEIVED_AT.plusSeconds(60);
        Instant wallTime = RECEIVED_AT.minusSeconds(3600);
        EventOrder order = normalizer.normalize(
                new EventClock(wallTime, 0), DEVICE_ID, OPERATION_ID, invalidObserved, RECEIVED_AT);

        assertThat(order.eventAt()).isEqualTo(RECEIVED_AT);
    }

    @Test
    void givenSameEventAt_whenComparingCanonical_thenTieBreaksByLogicalCounter() {
        EventOrder earlierCounter = new EventOrder(RECEIVED_AT, 0, DEVICE_ID, OPERATION_ID);
        EventOrder laterCounter = new EventOrder(RECEIVED_AT, 1, DEVICE_ID, OPERATION_ID);

        assertThat(EventOrder.CANONICAL.compare(earlierCounter, laterCounter)).isLessThan(0);
    }

    @Test
    void givenSameEventAtAndCounter_whenComparingCanonical_thenTieBreaksByDeviceIdBinaryOrder() {
        UUID lowerDevice = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID higherDevice = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");
        EventOrder lower = new EventOrder(RECEIVED_AT, 0, lowerDevice, OPERATION_ID);
        EventOrder higher = new EventOrder(RECEIVED_AT, 0, higherDevice, OPERATION_ID);

        assertThat(EventOrder.CANONICAL.compare(lower, higher)).isLessThan(0);
    }

    @Test
    void givenSameEventAtCounterAndDevice_whenComparingCanonical_thenTieBreaksByOperationIdBinaryOrder() {
        UUID lowerOperation = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID higherOperation = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");
        EventOrder lower = new EventOrder(RECEIVED_AT, 0, DEVICE_ID, lowerOperation);
        EventOrder higher = new EventOrder(RECEIVED_AT, 0, DEVICE_ID, higherOperation);

        assertThat(EventOrder.CANONICAL.compare(lower, higher)).isLessThan(0);
    }

    @Test
    void givenUuidsWithHighBitSet_whenComparingBinaryOrder_thenTreatsThemAsUnsigned() {
        UUID negativeMostSignificant = UUID.fromString("80000000-0000-0000-0000-000000000000");
        UUID zero = UUID.fromString("00000000-0000-0000-0000-000000000000");

        assertThat(EventOrder.compareUuidBinary(negativeMostSignificant, zero)).isGreaterThan(0);
    }
}
