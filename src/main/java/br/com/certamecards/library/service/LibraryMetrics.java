package br.com.certamecards.library.service;

import br.com.certamecards.common.error.ErrorCode;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class LibraryMetrics {

    private static final String SEARCH = "library.search";
    private static final String SUBSCRIPTION = "library.subscription";
    private static final String DUPLICATE = "library.duplicate";
    private static final String PREVIEW = "library.preview";

    private final MeterRegistry meterRegistry;

    public LibraryMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void search(boolean filtered, boolean empty) {
        meterRegistry
                .counter(SEARCH, "filtered", String.valueOf(filtered), "empty", String.valueOf(empty))
                .increment();
    }

    public void preview() {
        meterRegistry.counter(PREVIEW).increment();
    }

    public void subscription(String result) {
        meterRegistry.counter(SUBSCRIPTION, "result", result).increment();
    }

    public void subscriptionRejected(ErrorCode code) {
        if (code == ErrorCode.USER_CARD_LIMIT) {
            subscription("limit_rejected");
        }
        if (code == ErrorCode.DECK_NOT_AVAILABLE || code == ErrorCode.NOT_FOUND) {
            subscription("unavailable");
        }
    }

    public void duplicateRejected(boolean carryProgress, ErrorCode code) {
        if (code == ErrorCode.USER_CARD_LIMIT) {
            duplicate(carryProgress, "limit_rejected");
        }
    }

    public void duplicate(boolean carryProgress, String result) {
        String mode = carryProgress ? "carry" : "fresh";
        meterRegistry.counter(DUPLICATE, "mode", mode, "result", result).increment();
    }
}
