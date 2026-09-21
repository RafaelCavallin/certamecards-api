package br.com.certamecards.library.service;

import java.util.UUID;

public record ContentCursor(UUID after, int limit) {}
