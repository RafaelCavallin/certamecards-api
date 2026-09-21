package br.com.certamecards.events.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "product_events")
public class ProductEvent {

    @Id
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(nullable = false)
    private ProductEventName name;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private String props;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected ProductEvent() {}

    public ProductEvent(UUID id, UUID userId, ProductEventName name, String props, Instant occurredAt) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.props = props;
        this.occurredAt = occurredAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public ProductEventName getName() {
        return name;
    }

    public String getProps() {
        return props;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
