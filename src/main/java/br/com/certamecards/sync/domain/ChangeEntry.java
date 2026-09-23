package br.com.certamecards.sync.domain;

import tools.jackson.databind.util.RawValue;

public record ChangeEntry(long changeSeq, String type, RawValue payload) {}
