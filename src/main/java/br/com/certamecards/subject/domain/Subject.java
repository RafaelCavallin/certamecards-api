package br.com.certamecards.subject.domain;

import br.com.certamecards.common.sync.SyncMetadata;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "subjects")
public class Subject {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "normalized_name", nullable = false)
    private String normalizedName;

    @Column(nullable = false)
    private boolean active;

    @Embedded
    @AttributeOverride(name = "changeSeq", column = @Column(name = "change_seq"))
    private SyncMetadata syncMetadata = new SyncMetadata();

    protected Subject() {}

    public Subject(String name, String normalizedName) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.normalizedName = normalizedName;
        this.active = true;
    }

    public void rename(String name, String normalizedName) {
        this.name = name;
        this.normalizedName = normalizedName;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getNormalizedName() {
        return normalizedName;
    }

    public boolean isActive() {
        return active;
    }

    public Long getChangeSeq() {
        return syncMetadata.getChangeSeq();
    }
}
