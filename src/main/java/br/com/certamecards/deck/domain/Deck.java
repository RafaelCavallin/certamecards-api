package br.com.certamecards.deck.domain;

import br.com.certamecards.common.domain.ContentAudit;
import br.com.certamecards.common.sync.SyncMetadata;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "decks")
public class Deck {

    @Id
    private UUID id;

    @Column(name = "owner_id")
    private UUID ownerId;

    @Column(name = "subject_id", nullable = false)
    private UUID subjectId;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private DeckOrigin origin;

    @Column(name = "origin_ref")
    private UUID originRef;

    @Column(name = "search_text")
    private String searchText;

    @Embedded
    private DeckOfficialMeta officialMeta = new DeckOfficialMeta();

    @Embedded
    private ContentAudit audit = new ContentAudit();

    @Version
    private int version;

    @Embedded
    @AttributeOverride(name = "changeSeq", column = @Column(name = "change_seq"))
    private SyncMetadata syncMetadata = new SyncMetadata();

    protected Deck() {}

    public Deck(UUID id, UUID ownerId, UUID subjectId, String name) {
        this(id, ownerId, subjectId, name, DeckOrigin.OWN);
    }

    public Deck(UUID id, UUID ownerId, UUID subjectId, String name, DeckOrigin origin) {
        this.id = id;
        this.ownerId = ownerId;
        this.subjectId = subjectId;
        this.name = name;
        this.origin = origin;
    }

    public void rename(String name) {
        this.name = name;
    }

    public void changeDescription(String description) {
        this.description = description;
    }

    public void changeSubject(UUID subjectId) {
        this.subjectId = subjectId;
    }

    public void touch(Instant now) {
        audit.touch(now);
    }

    public void markDeleted(Instant now) {
        audit.markDeleted(now);
    }

    public boolean isDeleted() {
        return audit.getDeletedAt() != null;
    }

    public void updateSearchText(String searchText) {
        this.searchText = searchText;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public UUID getSubjectId() {
        return subjectId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public DeckOrigin getOrigin() {
        return origin;
    }

    public UUID getOriginRef() {
        return originRef;
    }

    public String getSearchText() {
        return searchText;
    }

    public DeckOfficialMeta getOfficialMeta() {
        return officialMeta;
    }

    public ContentAudit getAudit() {
        return audit;
    }

    public int getVersion() {
        return version;
    }

    public Long getChangeSeq() {
        return syncMetadata.getChangeSeq();
    }
}
