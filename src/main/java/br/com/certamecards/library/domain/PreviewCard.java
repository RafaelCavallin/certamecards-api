package br.com.certamecards.library.domain;

import java.util.UUID;

public record PreviewCard(UUID id, String front, String back, String source) {}
