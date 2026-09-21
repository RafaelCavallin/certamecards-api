package br.com.certamecards.events.domain;

public final class ProductEventLimits {

    public static final int MAX_EVENTS_PER_REQUEST = 50;
    public static final long MAX_REQUEST_BYTES = 16 * 1024L;
    public static final int MAX_PROPS_BYTES = 2 * 1024;
    public static final int MAX_REQUESTS_PER_MINUTE = 30;

    private ProductEventLimits() {}
}
