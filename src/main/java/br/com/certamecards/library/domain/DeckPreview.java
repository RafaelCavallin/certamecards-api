package br.com.certamecards.library.domain;

import java.util.List;

public record DeckPreview(LibraryDeckSummary deck, List<PreviewCard> cards) {}
