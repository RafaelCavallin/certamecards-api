package br.com.certamecards.sync.domain;

import java.util.UUID;

public record SubjectChange(UUID id, String name, boolean active, long changeSeq) {}
