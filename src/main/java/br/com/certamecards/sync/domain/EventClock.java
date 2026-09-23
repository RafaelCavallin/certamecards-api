package br.com.certamecards.sync.domain;

import java.time.Instant;

public record EventClock(Instant wallTime, int logicalCounter) {}
