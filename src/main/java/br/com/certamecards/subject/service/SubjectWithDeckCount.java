package br.com.certamecards.subject.service;

import br.com.certamecards.subject.domain.Subject;

public record SubjectWithDeckCount(Subject subject, long deckCount) {}
