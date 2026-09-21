package br.com.certamecards.subject.web;

import br.com.certamecards.subject.service.SubjectWithDeckCount;

public record SubjectAdminResponse(String id, String name, boolean active, Long changeSeq, long deckCount) {

    public static SubjectAdminResponse from(SubjectWithDeckCount item) {
        return new SubjectAdminResponse(
                item.subject().getId().toString(),
                item.subject().getName(),
                item.subject().isActive(),
                item.subject().getChangeSeq(),
                item.deckCount());
    }
}
