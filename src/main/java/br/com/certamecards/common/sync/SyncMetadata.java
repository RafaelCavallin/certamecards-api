package br.com.certamecards.common.sync;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class SyncMetadata {

    @Column(name = "change_seq", insertable = false, updatable = false)
    private Long changeSeq;

    public SyncMetadata() {}

    public Long getChangeSeq() {
        return changeSeq;
    }
}
