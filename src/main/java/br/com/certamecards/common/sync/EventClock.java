package br.com.certamecards.common.sync;

import java.time.Instant;

public record EventClock(Instant wallTime, int logicalCounter) {}
