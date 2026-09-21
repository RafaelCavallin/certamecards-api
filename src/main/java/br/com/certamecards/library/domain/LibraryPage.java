package br.com.certamecards.library.domain;

import java.util.List;

public record LibraryPage(List<LibraryDeckSummary> items, int page, int size, long total) {}
